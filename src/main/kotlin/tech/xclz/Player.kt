package tech.xclz

import io.ktor.utils.io.*
import tech.xclz.utils.readString

data class DeviceID(private val value: String) {
    override fun toString(): String = value
}

suspend inline fun ByteReadChannel.readDeviceID() = DeviceID(readString())

class Player(val deviceId: DeviceID) {
    var room: Room? = null
    var session: ClientSession? = null

    fun bindSession(session: ClientSession) {
        this.session = session
    }
}
