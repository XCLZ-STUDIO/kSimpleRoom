import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import tech.xclz.*

@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter : Router() {
    @Route(CommandID.Version)
    suspend fun version(session: ClientSession, deviceId: String): Int {
        if (deviceId in session.server.players) {
            TODO("Already in room, ready to recover the connection")
        }
        session.player = session.server.player(deviceId)    //bind player to session
        return SERVER_VERSION
    }
}

class RoomServer(// 构造函数
    var hostname: String = "0.0.0.0", var port: Int = 9999
) {
    var readyToConnect = false
    val players = mutableMapOf<String, Player>()

    fun player(deviceId: String) = players[deviceId] ?: Player(deviceId).apply { players[deviceId] = this }

    suspend fun start() {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
        readyToConnect = true
        while (true) {
            val socket = serverSocket.accept()
            CoroutineScope(currentCoroutineContext()).launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val session = ClientSession(this@RoomServer, socket, receiveChannel, sendChannel)

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