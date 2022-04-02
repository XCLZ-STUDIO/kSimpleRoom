package tech.xclz

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*


enum class CommandID(val id: UShort) {
    Version(0x0001u),
    CreateRoom(0x0002u),
    JoinRoom(0x0003u),
    LeaveRoom(0x0004u),
    PlayNote(0x0005u),
    StopNote(0x0006u);

    companion object {
        fun from(value: UShort) = values().first { it.id == value }
    }
}

annotation class Route(val cmd: CommandID)

abstract class Router {
    private val commands: MutableMap<CommandID, KFunction<*>> = mutableMapOf()

    init {
        val clazz = this::class
        for (function in clazz.declaredMemberFunctions) {
            function.findAnnotation<Route>()?.let {
                commands[it.cmd] = function
            }
        }
    }

    suspend fun dispatch(session: ClientSession, cmd: CommandID) {
        commands[cmd]?.let { function ->
            val instanceParam = function.instanceParameter ?: return@let
            val arguments = mutableMapOf<KParameter, Any>(
                instanceParam to this@Router
            )

            for (param in function.valueParameters) {
                val type = param.type.classifier as KClass<*>
                val value: Any = when (type) {
                    ClientSession::class -> session
                    UShort::class -> session.receiveChannel.readUShort()
                    UInt::class -> session.receiveChannel.readUInt()
                    String::class -> session.receiveChannel.readString()
                    DeviceID::class -> DeviceID(session.receiveChannel.readString())
                    RoomID::class -> RoomID(session.receiveChannel.readString())
                    else -> handleOtherReadTypes(session, type)
                }
                arguments[param] = value
            }

            val result = function.callSuspendBy(arguments)
            val type = function.returnType.classifier as KClass<*>
            when (type) {
                Boolean::class -> session.sendChannel.writeByte(if (result as Boolean) 1 else 0)
                Int::class -> session.sendChannel.writeInt(result as Int)
                UShort::class -> session.sendChannel.writeUShort(result as UShort)
                String::class -> session.sendChannel.writeString(result as String)
                DeviceID::class -> session.sendChannel.writeString((result as DeviceID).toString())
                RoomID::class -> session.sendChannel.writeString((result as RoomID).toString())
                Unit::class -> Unit
                else -> handleOtherWriteTypes(session, type)
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
