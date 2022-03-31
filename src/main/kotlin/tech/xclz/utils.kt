package tech.xclz

import io.ktor.utils.io.*
import java.nio.ByteBuffer
import java.nio.charset.Charset


suspend fun ByteWriteChannel.writeUShort(s: UShort) = this.writeShort(s.toShort())
suspend fun ByteWriteChannel.writeString(s: String) {
    this.writeInt(s.length)
    this.writeFully(s.toByteArray())
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()
suspend fun ByteReadChannel.readUInt(): UInt = this.readInt().toUInt()
suspend fun ByteReadChannel.readBytes(size: Int): ByteArray {
    val buffer = ByteBuffer.allocate(size)
    this.readFully(buffer)
    buffer.flip()
    return ByteArray(size) { buffer.get() }
}

suspend fun ByteReadChannel.readString(size: Int, charset: Charset = Charsets.UTF_8): String =
    this.readBytes(size).toString(charset)

