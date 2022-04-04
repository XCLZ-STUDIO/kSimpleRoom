package tech.xclz.core

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
open class BasicTCPServer(// 构造函数
    hostname: String = "0.0.0.0", port: Int = 9999
) {
    private lateinit var sockets: ReceiveChannel<Socket>
    protected var serverSocket: ServerSocket

    init {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
    }

    fun CoroutineScope.produceSocketBy(producer: suspend (server: ServerSocket, ProducerScope<Socket>) -> Unit) {
        sockets = produce {
            while (true) {
                producer(serverSocket, this)
            }
        }
    }

    fun CoroutineScope.consumeSocketWith(consumer: suspend (socket: Socket) -> Unit) =
        launch {
            while (true) {
                sockets.consumeEach { socket ->
                    launch {
                        consumer(socket)
                    }
                }
            }
        }
}