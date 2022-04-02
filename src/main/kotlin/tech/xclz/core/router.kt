package tech.xclz.core

import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.xclz.ClientSession
import tech.xclz.DeviceID
import tech.xclz.readDeviceID
import tech.xclz.utils.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

enum class CommandType(val value: UShort) {
    GetVersion(0x0001U),
    CreateRoom(0x0002U),
    JoinRoom(0x0003U),
    LeaveRoom(0x0004U),
    SendMessage(0x0005U),
    BindDevice(0x0006U);

    companion object {
        fun from(value: UShort) = values().first { it.value == value }
    }
}

suspend inline fun ByteReadChannel.readCommandType(): CommandType = CommandType.from(readUShort())
suspend inline fun ByteWriteChannel.writeCommandType(type: CommandType) = writeUShort(type.value)

class Command(val type: CommandType, val askId: UShort, val function: KFunction<*>, val arguments: Map<KParameter, Any>)

annotation class Route(val cmd: CommandType)

abstract class Router {
    private val commands: MutableMap<CommandType, KFunction<*>> = mutableMapOf()

    init {
        val clazz = this::class
        for (function in clazz.declaredMemberFunctions) {
            function.findAnnotation<Route>()?.let {
                commands[it.cmd] = function
            }
        }
    }

    private suspend fun readCommand(session: ClientSession): Command? {
        //其实有可能不需要这个锁，因为目前只有这里会从socket内读取内容，
        session.withCommandLock {
            val receiveChannel = session.receiveChannel
            val type = receiveChannel.readCommandType()
            val askId = receiveChannel.readUShort()

            commands[type]?.let { function ->
                val instanceParam = function.instanceParameter ?: return@let
                val arguments = mutableMapOf<KParameter, Any>(
                    instanceParam to this@Router
                )

                for (param in function.valueParameters) {
                    val paramType = param.type.classifier as KClass<*>
                    val value: Any = when (paramType) {
                        ClientSession::class -> session
                        UInt::class -> receiveChannel.readUInt()
                        Short::class -> receiveChannel.readShort()
                        RoomID::class -> receiveChannel.readRoomID()
                        String::class -> receiveChannel.readString()
                        UShort::class -> receiveChannel.readUShort()
                        DeviceID::class -> receiveChannel.readDeviceID()
                        else -> handleOtherReadTypes(session, paramType)
                    }
                    arguments[param] = value
                }

                return Command(type, askId, function, arguments)
            }
        }
        return null
    }

    private suspend fun execute(session: ClientSession, command: Command) {
        val result = command.function.callSuspendBy(command.arguments)

        session.withReturnLock {
            val sendChannel = session.sendChannel
            sendChannel.writeCommandType(command.type)
            sendChannel.writeUShort(command.askId)

            val type = command.function.returnType.classifier as KClass<*>
            when (type) {
                Unit::class -> Unit
                Int::class -> sendChannel.writeInt(result as Int)
                Short::class -> sendChannel.writeShort(result as Short)
                UShort::class -> sendChannel.writeUShort(result as UShort)
                String::class -> sendChannel.writeString(result as String)
                RoomID::class -> sendChannel.writeString((result as RoomID).toString())
                Boolean::class -> sendChannel.writeByte(if (result as Boolean) 1 else 0)
                DeviceID::class -> sendChannel.writeString((result as DeviceID).toString())
                else -> handleOtherWriteTypes(session, type)
            }
        }
    }

    suspend fun dispatch(session: ClientSession) = coroutineScope {
        while (true) {
            //TODO 处理读取命令发生错误的情况
            readCommand(session)?.let {
                launch {
                    execute(session, it)
                }
            }
        }
    }

    protected open suspend fun handleOtherReadTypes(session: ClientSession, type: KClass<*>) {
        throw IllegalArgumentException("Unsupported type: $type")
    }

    protected open suspend fun handleOtherWriteTypes(session: ClientSession, type: KClass<*>) {
        throw IllegalArgumentException("Unsupported type: $type")
    }
}
