package tech.xclz.core

import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tech.xclz.ClientSession
import tech.xclz.DeviceID
import tech.xclz.readDeviceID
import tech.xclz.utils.*
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

enum class CommandType(val value: UShort) {
    GetVersion(0x0001u),
    CreateRoom(0x0002u),
    JoinRoom(0x0003u),
    LeaveRoom(0x0004u),
    SendMessage(0x0005u),
    BindDevice(0x0006u);

    companion object {
        fun from(value: UShort) = values().firstOrNull { it.value == value }
    }
}

suspend inline fun ByteReadChannel.readCommandType(): CommandType? = CommandType.from(readUShort())
suspend inline fun ByteWriteChannel.writeCommandType(type: CommandType) = writeUShort(type.value)

class Command(
    val type: CommandType,
    val askId: UShort,
    val function: KFunction<*>,
    val arguments: Map<KParameter, Any>
) {
    override fun toString(): String {
        return "Command(type=$type, askId=$askId)"
    }
}

enum class MessageType(val value: UShort) {
    PlayNote(0x0001u),
    StopNote(0x0002u);

    companion object {
        fun from(value: UShort) = values().firstOrNull { it.value == value }
    }
}

suspend inline fun ByteReadChannel.readMessageType(): MessageType? = MessageType.from(readUShort())
suspend inline fun ByteWriteChannel.writeMessageType(type: MessageType) = writeUShort(type.value)

abstract class SyncMessage(val frameID: UInt, val type: MessageType, val time: Long)
class PlayNoteMessage(frameID: UInt, time: Long, val note: UByte) : SyncMessage(frameID, MessageType.PlayNote, time) {
    override fun toString(): String {
        return "PlayNoteMessage(time=$time, note=$note)"
    }
}
class StopNoteMessage(frameID: UInt, time: Long, val note: UByte) : SyncMessage(frameID, MessageType.PlayNote, time) {
    override fun toString(): String {
        return "StopNoteMessage(time=$time, note=$note)"
    }
}

annotation class Route(val type: CommandType)

abstract class Router {
    private val functions: MutableMap<CommandType, KFunction<*>> = mutableMapOf()

    init {
        val clazz = this::class
        for (function in clazz.declaredMemberFunctions) {
            function.findAnnotation<Route>()?.let {
                functions[it.type] = function
            }
        }
    }

    private suspend fun readCommand(session: ClientSession, type: CommandType): Command? {
        //其实有可能不需要这个锁，因为目前只有这里会从socket内读取内容，
        session.withReceiveLock { receiveChannel ->
            logger.debug("读取命令头")
            val askId = receiveChannel.readUShort()
            logger.debug("读取到: type=$type askId=$askId")

            functions[type]?.let { function ->
                val instanceParam = function.instanceParameter ?: TODO("如果instanceParam为空怎么办")
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

    private suspend fun readSyncMessage(session: ClientSession): SyncMessage {
        session.withReceiveLock { receiveChannel ->
            val frameID = receiveChannel.readUInt()
            val type = receiveChannel.readMessageType()
                ?: TODO("如果type为空怎么办")
            val time = receiveChannel.readLong()

            return when (type) {
                MessageType.PlayNote -> {
                    val note = receiveChannel.readUByte()
                    PlayNoteMessage(frameID, time, note)
                }
                MessageType.StopNote -> {
                    val note = receiveChannel.readUByte()
                    StopNoteMessage(frameID, time, note)
                }
            }
        }
    }

    private suspend fun execute(session: ClientSession, command: Command) {
        logger.debug("服务器开始执行命令: $command")
        try {
            val result = command.function.callSuspendBy(command.arguments)
            logger.debug("服务器执行命令已获取返回值: $result")

            session.withSendLock { sendChannel ->
                logger.debug("服务器开始发送命令返回值")
                sendChannel.writeCommandType(command.type)
                sendChannel.writeUShort(command.askId)

                val type = command.function.returnType.classifier as KClass<*>
                when (type) {
                    Unit::class -> Unit
                    Int::class -> sendChannel.writeInt(result as Int)
                    Byte::class -> sendChannel.writeByte(result as Byte)
                    Short::class -> sendChannel.writeShort(result as Short)
                    UShort::class -> sendChannel.writeUShort(result as UShort)
                    String::class -> sendChannel.writeString(result as String)
                    RoomID::class -> sendChannel.writeRoomID(result as RoomID)
                    Boolean::class -> sendChannel.writeBoolean(result as Boolean)
                    DeviceID::class -> sendChannel.writeString((result as DeviceID).toString())
                    else -> handleOtherWriteTypes(session, type)
                }
            }
        } catch (e: InvocationTargetException) {
            throw e
        }
    }

    suspend fun dispatch(session: ClientSession) = coroutineScope {
        while (true) {
            //TODO 处理读取命令发生错误的情况
            logger.debug("服务器开始读取客户端命令")
            val type = session.receiveChannel.readCommandType()
                ?: TODO("如果type为空怎么办")

            if (type == CommandType.SendMessage) {
                logger.debug("服务器开始读取客户端发送的消息")
                val message = readSyncMessage(session)
                logger.debug("服务器读取客户端发送的消息完成，消息为: $message，开始处理")
                handleMessage(session, message)
            } else {
                readCommand(session, type)?.let {
                    logger.debug("服务器已读取到客户端命令: $it，并开始异步执行")
                    launch {
                        execute(session, it)
                        logger.debug("客户端命令: $it 异步执行完成")
                    }
                }
            }
        }
    }

    protected abstract suspend fun handleMessage(session: ClientSession, message: SyncMessage)

    protected open suspend fun handleOtherReadTypes(session: ClientSession, type: KClass<*>) {
        throw IllegalArgumentException("Unsupported type: $type")
    }

    protected open suspend fun handleOtherWriteTypes(session: ClientSession, type: KClass<*>) {
        throw IllegalArgumentException("Unsupported type: $type")
    }
}
