package tech.xclz.korom.server.entity

import tech.xclz.korom.protocol.DeviceID
import tech.xclz.korom.server.ClientSession

class Player(val deviceId: DeviceID) {
    var room: Room? = null
    var session: ClientSession? = null

    fun bindSession(session: ClientSession) {
        this.session = session
    }
}
