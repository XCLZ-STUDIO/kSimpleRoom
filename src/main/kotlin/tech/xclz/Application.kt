//@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package tech.xclz

import io.ktor.network.sockets.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import tech.xclz.core.BasicTCPServer
import tech.xclz.core.RoomID

@OptIn(ExperimentalCoroutinesApi::class)
class RoomServer(var hostname: String = "0.0.0.0", var port: Int = 9999) :
    BasicTCPServer(hostname, port) {
    private val players = mutableMapOf<DeviceID, Player>()
    private val rooms = mutableMapOf<RoomID, Room>()

    fun room(code: RoomID) = rooms[code] ?: Room(code).also { rooms[code] = it }
    fun player(deviceId: DeviceID) = players[deviceId] ?: Player(deviceId).also { players[deviceId] = it }

    suspend fun start() {
        coroutineScope {
            produceSocketBy { server, producer ->
                val socket = server.accept()
                producer.send(socket)
            }

            consumeSocketWith { socket ->
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val session = ClientSession(this@RoomServer, socket, receiveChannel, sendChannel)

                TCPRouter.dispatch(session)
            }
        }
    }

    // 判断玩家是否已连接服务器
    operator fun contains(deviceId: DeviceID): Boolean {
        return deviceId in players
    }
}


fun main() {
    runBlocking {
        RoomServer().start()
    }
}