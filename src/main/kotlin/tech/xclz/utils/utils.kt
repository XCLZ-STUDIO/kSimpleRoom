package tech.xclz.utils

import io.ktor.utils.io.*
import java.nio.ByteBuffer
import java.nio.charset.Charset


suspend inline fun ByteWriteChannel.writeUShort(s: UShort) = this.writeShort(s.toShort())
suspend inline fun ByteWriteChannel.writeString(s: String) {
    this.writeInt(s.length)
    this.writeFully(s.toByteArray())
}

suspend inline fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()
suspend inline fun ByteReadChannel.readUInt(): UInt = this.readInt().toUInt()
suspend inline fun ByteReadChannel.readBytes(size: Int): ByteArray {
    val buffer = ByteBuffer.allocate(size)
    this.readFully(buffer)
    buffer.flip()
    return ByteArray(size) { buffer.get() }
}

suspend inline fun ByteReadChannel.readString(size: Int, charset: Charset = Charsets.UTF_8): String =
    this.readBytes(size).toString(charset)
suspend inline fun ByteReadChannel.readString(charset: Charset = Charsets.UTF_8): String =
    this.readString(this.readInt(), charset)
