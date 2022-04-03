package tech.xclz

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.xclz.PlayerAction.*
import tech.xclz.PlayerState.*
import tech.xclz.core.Actionizable
import tech.xclz.core.DefaultState.End
import tech.xclz.core.DefaultState.Start
import tech.xclz.core.RoomIDManager
import tech.xclz.core.buildStateMachine
import tech.xclz.utils.logger

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
        val room = server.room(roomID)

        //FIXME 如果玩家未与会话绑定呢？
        player?.let {
            room.addPlayer(it)
            it.room = room
        }

        stateOn(create)
        logger.debug { "[${player?.deviceId}] 创建房间完成" }
        return player?.room
    }

    fun joinRoom(room: Room) {
        //FIXME 如果玩家未与会话绑定呢？
        player?.let { player ->
            room.addPlayer(player)
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
