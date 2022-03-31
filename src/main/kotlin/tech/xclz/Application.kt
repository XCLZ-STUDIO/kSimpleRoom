import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
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

@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter {
    val commands: MutableMap<CommandID, KFunction<*>> = mutableMapOf()

    init {
        val clazz = this::class
        for (function in clazz.declaredMemberFunctions) {
            function.findAnnotation<Route>()?.let {
                commands[it.cmd] = function
            }
        }
    }

    @Route(CommandID.Version)
    suspend fun version(receiveChannel: ByteReadChannel, deviceId: String) {
        println("收到deviceId: $deviceId")
    }
}

suspend fun server() {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 9998)
    while (true) {
        val socket = serverSocket.accept()
        CoroutineScope(currentCoroutineContext()).launch {
            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            while (true) {
                val id = receiveChannel.readUShort()
                val cmd = CommandID.from(id)

                TCPRouter.commands[cmd]?.let { function ->
                    val instanceParam = function.instanceParameter ?: return@let
                    val arguments = mutableMapOf<KParameter, Any>(
                        instanceParam to TCPRouter
                    )

                    for (parameter in function.valueParameters) {
                        val type = parameter.type.classifier as KClass<*>
                        val value: Any = when (type) {
                            ByteReadChannel::class -> receiveChannel
                            UShort::class -> receiveChannel.readUShort()
                            UInt::class -> receiveChannel.readUInt()
                            String::class -> receiveChannel.readString(receiveChannel.readInt())
                            else -> throw IllegalArgumentException("Unsupported type: $type")
                        }
                        arguments[parameter] = value
                    }

                    function.callSuspendBy(arguments)
                }
            }
        }
    }
}

suspend fun client() {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 9998)

    val receiveChannel = socket.openReadChannel()
    val sendChannel = socket.openWriteChannel(autoFlush = true)

    sendChannel.writeUShort(CommandID.Version.id)
    sendChannel.writeString("abcdefg")
}

fun main() {
    runBlocking {
        launch { server() }
        client()
    }
}