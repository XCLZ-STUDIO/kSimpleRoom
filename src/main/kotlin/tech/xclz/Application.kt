@file:OptIn(ExperimentalCoroutinesApi::class)

package tech.xclz

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce

class RoomServer(// 构造函数
    var hostname: String = "0.0.0.0", var port: Int = 9999
) {
    val players = mutableMapOf<String, Player>()
    val rooms = mutableMapOf<RoomID, Room>()

    lateinit var sockets: ReceiveChannel<Socket>
    private var serverSocket: ServerSocket

    fun room(code: RoomID) = rooms[code] ?: Room(code).also { rooms[code] = it }
    fun player(deviceId: String) = players[deviceId] ?: Player(deviceId).also { players[deviceId] = it }

    init {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
    }

    suspend fun start() {
        produceSocketBy { producer ->
            val socket = serverSocket.accept()
            producer.send(socket)
        }

        consumeSocketWith { socket ->
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

    private suspend fun produceSocketBy(producer: suspend (ProducerScope<Socket>) -> Unit) {
        CoroutineScope(currentCoroutineContext()).launch {
            sockets = produce {
                while (true) {
                    producer(this)
                }
            }
        }
    }

    private suspend fun consumeSocketWith(consumer: suspend (socket: Socket) -> Unit) {
        CoroutineScope(currentCoroutineContext()).launch {
            while (true) {
                sockets.consumeEach { socket ->
                    launch {
                        consumer(socket)
                    }
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