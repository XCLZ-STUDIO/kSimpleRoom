package tech.xclz

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.xclz.PlayerAction.*
import tech.xclz.PlayerState.*
import tech.xclz.core.DefaultState.End
import tech.xclz.core.DefaultState.Start
import tech.xclz.core.RoomID
import tech.xclz.core.RoomIDManager
import tech.xclz.core.buildStateMachine

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
    val commandMutex = Mutex()
    val returnMutex = Mutex()

    //bind a player to self
    fun connect(deviceId: DeviceID) {
        this.player = server.player(deviceId).also { player ->
            player.bindSession(this)
        }
        state.on(connect)
    }

    fun createRoom() {
        val code = RoomIDManager.getRoomID()
        val room = server.room(code)

        //FIXME 如果玩家未与会话绑定呢？
        player?.let {
            room.addPlayer(it)
            it.room = room
        }

        state.on(create)
    }

    fun joinRoom(code: RoomID) {
        //FIXME 如果玩家未与会话绑定呢？
        player?.let { player ->
            player.room = server.room(code).also {
                it.addPlayer(player)
            }
        }
        state.on(join)
    }

    fun leaveRoom() {
        player?.let { player ->
            player.room?.removePlayer(player)
            player.room = null
        }
        state.on(leave)
    }

    suspend inline fun <T> withCommandLock(action: () -> T): T = commandMutex.withLock(action = action)
    suspend inline fun <T> withReturnLock(action: () -> T): T = returnMutex.withLock(action = action)
}
