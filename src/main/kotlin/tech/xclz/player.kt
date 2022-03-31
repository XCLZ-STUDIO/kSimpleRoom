package tech.xclz

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

class Player(val server: RoomServer, val deviceId: String) {
    var room: Room? = null
    var session: ClientSession? = null
    var state = playerStateMachine.initState

    fun bindSession(session: ClientSession) {
        this.session = session
        state.on("connect")
    }

    fun createRoom() {
        val code = "XXXX"   //FIXME 随机生成房间号
        this.room = server.room(code).also {
            it.addPlayer(this)
        }
        state.on("create")
    }

    fun joinRoom(code: String) {
        this.room = server.room(code).also {
            it.addPlayer(this)
        }
        state.on("join")
    }

    fun leaveRoom() {
        this.room?.removePlayer(this)
        this.room = null
        state.on("leave")
    }
}
