package tech.xclz

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*

class RoomServer(// 构造函数
    var hostname: String = "0.0.0.0", var port: Int = 9999
) {
    val players = mutableMapOf<String, Player>()
    val rooms = mutableMapOf<String, Room>()

    fun room(code: String) = rooms[code] ?: Room(code).also { rooms[code] = it }
    fun player(deviceId: String) =
        players[deviceId] ?: Player(this, deviceId).also { players[deviceId] = it }

    suspend fun start() {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
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