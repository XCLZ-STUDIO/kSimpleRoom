package tech.xclz

class Player(val deviceId: String) {
    var room: Room? = null
    var session: ClientSession? = null

    fun bindSession(session: ClientSession) {
        this.session = session
    }
}
