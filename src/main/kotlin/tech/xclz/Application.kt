//@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package tech.xclz

import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import tech.xclz.core.BasicTCPServer
import tech.xclz.core.RoomID
import tech.xclz.core.RoomIDManager
import tech.xclz.utils.logger

@OptIn(ExperimentalCoroutinesApi::class)
class RoomServer(var hostname: String = "0.0.0.0", var port: Int = 9999) :
    BasicTCPServer(hostname, port) {
    private val players = mutableMapOf<DeviceID, Player>()
    private val rooms = mutableMapOf<RoomID, Room>()

    fun room(code: RoomID) = rooms[code] ?: Room(code).also { rooms[code] = it }
    fun player(deviceId: DeviceID) = players[deviceId] ?: Player(deviceId).also { players[deviceId] = it }

    suspend fun start() = coroutineScope {
        withContext(Dispatchers.Default) {
            RoomIDManager   // 初始化房间ID管理器
        }

        produceSocketBy { server, producer ->
            val socket = server.accept()
            producer.send(socket)
        }

        consumeSocketWith { socket ->
            logger.debug {
                val port = (socket.remoteAddress as InetSocketAddress).port
                "Socket已连接，端口: $port"
            }
            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)
            logger.debug("已打开读写通道")
            val session = ClientSession(this@RoomServer, socket, receiveChannel, sendChannel)

            logger.debug("开始分发路由")
            TCPRouter.dispatch(session)
        }
    }

    // 判断玩家是否已连接服务器
    operator fun contains(deviceID: DeviceID): Boolean = players.contains(deviceID)
}

fun main() {
    runBlocking {
        withContext(Dispatchers.IO) {
            RoomServer().start()
        }
    }
}