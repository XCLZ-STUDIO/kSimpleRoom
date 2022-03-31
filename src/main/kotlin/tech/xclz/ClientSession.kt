package tech.xclz

import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class ClientSession(
    val socket: Socket,
    val receiveChannel: ByteReadChannel,
    val sendChannel: ByteWriteChannel
) {
//    constructor(socket: Socket) : this(socket, socket.openReadChannel(), socket.openWriteChannel(autoFlush = true))
}
