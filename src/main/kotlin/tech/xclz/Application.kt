import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import tech.xclz.ClientSession
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

enum class CommandID(val id: UShort) {
    Version(0x0001u),
    CreateRoom(0x0002u),
    JoinRoom(0x0003u),
    LeaveRoom(0x0004u);

    companion object {
        fun from(value: UShort) = values().first { it.id == value }
    }
}

suspend fun ByteWriteChannel.writeUShort(s: UShort) = this.writeShort(s.toShort())
suspend fun ByteWriteChannel.writeString(s: String) {
    this.writeInt(s.length)
    this.writeFully(s.toByteArray())
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()
suspend fun ByteReadChannel.readUInt(): UInt = this.readInt().toUInt()
suspend fun ByteReadChannel.readBytes(size: Int): ByteArray {
    val buffer = ByteBuffer.allocate(size)
    this.readFully(buffer)
    buffer.flip()
    return ByteArray(size) { buffer.get() }
}

suspend fun ByteReadChannel.readString(size: Int, charset: Charset = Charsets.UTF_8): String =
    this.readBytes(size).toString(charset)


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
                instanceParam to TCPRouter
            )

            for (parameter in function.valueParameters) {
                val type = parameter.type.classifier as KClass<*>
                val value: Any = when (type) {
                    ByteReadChannel::class -> session.receiveChannel
                    UShort::class -> session.receiveChannel.readUShort()
                    UInt::class -> session.receiveChannel.readUInt()
                    String::class -> session.receiveChannel.readString(session.receiveChannel.readInt())
                    else -> throw IllegalArgumentException("Unsupported type: $type")
                }
                arguments[parameter] = value
            }

            val result = function.callSuspendBy(arguments)
            val type = function.returnType.classifier as KClass<*>
            when (type) {
                Int::class -> session.sendChannel.writeInt(result as Int)
                UShort::class -> session.sendChannel.writeUShort(result as UShort)
                String::class -> session.sendChannel.writeString(result as String)
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        }
    }
}

@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter: Router() {
    @Route(CommandID.Version)
    suspend fun version(deviceId: String) {
        println("收到deviceId: $deviceId")
    }
}

class RoomServer(// 构造函数
    var hostname: String = "0.0.0.0", var port: Int = 9999
) {
    var readyToConnect = false

    suspend fun start() {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
        readyToConnect = true
        while (true) {
            val socket = serverSocket.accept()
            CoroutineScope(currentCoroutineContext()).launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val session = ClientSession(socket, receiveChannel, sendChannel)

                while (true) {
                    val id = receiveChannel.readUShort()
                    val cmd = CommandID.from(id)

                    TCPRouter.dispatch(session, cmd)
                }
            }
        }
    }

}


fun main() {
    runBlocking {
        launch {
            RoomServer().start()
        }
    }
}