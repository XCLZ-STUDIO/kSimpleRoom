package tech.xclz

import io.ktor.utils.io.*
import java.nio.ByteBuffer

//suspend fun ByteReadChannel.readBytes(size: Int): ByteArray {
//    val buffer = ByteBuffer.allocate(size)
//    this.readFully(buffer)
//    buffer.flip()
//    return ByteArray(size) { buffer.get() }
//}
//
//suspend fun ByteReadChannel.readString(size: Int): String =
//    readBytes(size).toString(Charsets.UTF_8)
