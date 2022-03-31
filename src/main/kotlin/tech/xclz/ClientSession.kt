package tech.xclz

import io.ktor.network.sockets.*
import io.ktor.utils.io.*


val playerStateMachine = buildStateMachine {
    "*" by {
        "connect" goto "NotInRoom"
    }

    "NotInRoom" by {
        "create" goto "Manager"
        "join" goto "Member"
        "disconnect" goto "*"
    }

    "Manager" by {
        "leave" goto "NotInRoom"
        "disconnect" goto "ManagerIDLE"
    }

    "Member" by {
        "leave" goto "NotInRoom"
        "disconnect" goto "MemberIDLE"
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

    //bind a player to self
    fun connect(deviceId: String) {
        this.player = server.player(deviceId).also { player ->
            player.bindSession(this)
        }
        state.on("connect")
    }

    fun createRoom() {
        val code = RoomIDManager.getRoomID()
        val room = server.room(code)

        //FIXME 如果玩家未与会话绑定呢？
        player?.let {
            room.addPlayer(it)
            it.room = room
        }

        state.on("create")
    }

    fun joinRoom(code: RoomID) {
        //FIXME 如果玩家未与会话绑定呢？
        player?.let { player ->
            player.room = server.room(code).also {
                it.addPlayer(player)
            }
        }
        state.on("join")
    }

    fun leaveRoom() {
        player?.let { player ->
            player.room?.removePlayer(player)
            player.room = null
        }
        state.on("leave")
    }
}
