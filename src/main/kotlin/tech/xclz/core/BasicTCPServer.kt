@file:OptIn(ExperimentalCoroutinesApi::class)

package tech.xclz.core

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce

open class BasicTCPServer(// 构造函数
    hostname: String = "0.0.0.0", port: Int = 9999
) {
    private lateinit var sockets: ReceiveChannel<Socket>
    protected var serverSocket: ServerSocket

    init {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
    }

    suspend fun produceSocketBy(producer: suspend (server: ServerSocket, ProducerScope<Socket>) -> Unit) {
        CoroutineScope(currentCoroutineContext()).launch {
            sockets = produce {
                while (true) {
                    producer(serverSocket, this)
                }
            }
        }
    }

    suspend fun consumeSocketWith(consumer: suspend (socket: Socket) -> Unit) {
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