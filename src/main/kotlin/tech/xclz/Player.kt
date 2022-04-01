package tech.xclz

class DeviceID(private val id: String) {
    override fun toString(): String {
        return id
    }
}

class Player(val deviceId: DeviceID) {
    var room: Room? = null
    var session: ClientSession? = null

    fun bindSession(session: ClientSession) {
        this.session = session
    }
}
