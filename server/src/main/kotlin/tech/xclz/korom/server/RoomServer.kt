package tech.xclz.korom.server

import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.xclz.korom.protocol.DeviceID
import tech.xclz.korom.protocol.RoomID
import tech.xclz.korom.server.entity.Player
import tech.xclz.korom.server.entity.Room
import tech.xclz.korom.server.entity.RoomIDManager
import tech.xclz.korom.server.route.TCPRouter

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