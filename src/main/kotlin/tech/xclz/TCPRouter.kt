package tech.xclz


@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter : Router() {
    @Route(CommandID.Version)
    suspend fun version(session: ClientSession, deviceId: DeviceID): Int {
        if (deviceId in session.server) {
            TODO("Already in room, ready to recover the connection")
        }
        session.connect(deviceId)    //bind player to session
        return SERVER_VERSION
    }

    @Route(CommandID.CreateRoom)
    suspend fun createRoom(session: ClientSession, roomId: RoomID): Boolean {
        session.createRoom()
        return true
    }

    @Route(CommandID.JoinRoom)
    suspend fun joinRoom(session: ClientSession, roomId: RoomID): Boolean {
        session.joinRoom(roomId)
        return true
    }

    @Route(CommandID.LeaveRoom)
    suspend fun leaveRoom(session: ClientSession): Boolean {
        session.leaveRoom()
        return true
    }
}
