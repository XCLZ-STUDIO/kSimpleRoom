package tech.xclz

import tech.xclz.core.CommandType
import tech.xclz.core.RoomID
import tech.xclz.core.Route
import tech.xclz.core.Router


@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter : Router() {
    @Route(CommandType.GetVersion)
    suspend fun version(session: ClientSession, deviceId: DeviceID): Short {
        if (deviceId in session.server) {
            TODO("Already in room, ready to recover the connection")
        }
        session.connect(deviceId)    //bind player to session
        return SERVER_VERSION.toShort()
    }

    @Route(CommandType.CreateRoom)
    suspend fun createRoom(session: ClientSession, roomId: RoomID): Boolean {
        session.createRoom()
        return true
    }

    @Route(CommandType.JoinRoom)
    suspend fun joinRoom(session: ClientSession, roomId: RoomID): Boolean {
        session.joinRoom(roomId)
        return true
    }

    @Route(CommandType.LeaveRoom)
    suspend fun leaveRoom(session: ClientSession): Boolean {
        session.leaveRoom()
        return true
    }
}
