package tech.xclz

import RoomServer
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class ClientSession(
    val server: RoomServer,
    val socket: Socket,
    val receiveChannel: ByteReadChannel,
    val sendChannel: ByteWriteChannel
) {
    var player: Player? = null
}
