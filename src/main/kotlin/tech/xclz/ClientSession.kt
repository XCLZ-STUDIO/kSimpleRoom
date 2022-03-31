package tech.xclz

import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class ClientSession(
    val server: RoomServer,
    val socket: Socket,
    val receiveChannel: ByteReadChannel,
    val sendChannel: ByteWriteChannel
) {
    var player: Player? = null

    //bind a player to self
    fun player(deviceId: String) = server.player(deviceId).also {
        this.player = it
        it.bindSession(this)
    }
}
