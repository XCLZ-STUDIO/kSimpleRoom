package tech.xclz.korom.server

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.xclz.korom.server.PlayerAction.*
import tech.xclz.korom.server.PlayerState.*
import tech.xclz.korom.protocol.DeviceID
import tech.xclz.korom.server.DefaultState.End
import tech.xclz.korom.server.DefaultState.Start
import tech.xclz.korom.server.entity.Player
import tech.xclz.korom.server.entity.Room
import tech.xclz.korom.server.entity.RoomIDManager

val playerStateMachine = buildStateMachine {
    Start by {
        connect goto NotInRoom
    }

    NotInRoom by {
        create goto Manager
        join goto Member
        disconnect goto End
    }

    Manager by {
        leave goto NotInRoom
        disconnect goto ManagerIDLE
    }

    Member by {
        leave goto NotInRoom
        disconnect goto MemberIDLE
    }
}

class ClientSession(
    val server: RoomServer,
    val socket: Socket,
    val receiveChannel: ByteReadChannel,
    val sendChannel: ByteWriteChannel
) {
    var player: Player? = null
    var state = playerStateMachine.initState
    val receiveMutex = Mutex()
    val sendMutex = Mutex()
    val room: Room?
        get() = player?.room

    //bind a player to self
    fun connect(deviceID: DeviceID) {
        val player = server.player(deviceID)
        player.bindSession(this)
        this.player = player
        stateOn(connect)
    }

    fun createRoom(): Room? {
        logger.debug { "[${player?.deviceId}] 开始创建房间" }
        val roomID = RoomIDManager.getRoomID()
        logger.debug { "[${player?.deviceId}] 获取到房间ID: $roomID" }
        val room = server.room(roomID) //FIXME 如果该房间已存在呢？

        //FIXME 如果玩家未与会话绑定呢？
        player?.let {
            room.addPlayer(it)
            it.room = room
        }

        stateOn(create)
        logger.info { "[${player?.deviceId}] 创建房间完成，房间起始时间: ${room.startTime}" }
        return player?.room
    }

    fun joinRoom(room: Room) {
        //FIXME 如果玩家未与会话绑定呢？
        player?.let { player ->
            room.addPlayer(player)  //FIXME 如果该玩家已在房间中呢？或已在另一个房间中？
            player.room = room
        }
        stateOn(join)
    }

    fun leaveRoom() {
        player?.let { player ->
            player.room?.removePlayer(player)
            player.room = null
        }
        stateOn(leave)
    }

    suspend inline fun <T> withReceiveLock(action: (ByteReadChannel) -> T): T =
        receiveMutex.withLock { action(receiveChannel) }

    suspend inline fun <T> withSendLock(action: (ByteWriteChannel) -> T): T =
        sendMutex.withLock {
            action(sendChannel).also { sendChannel.flush() }
        }

    private fun stateOn(action: Actionizable) {
        state = state.on(action)
    }
}
