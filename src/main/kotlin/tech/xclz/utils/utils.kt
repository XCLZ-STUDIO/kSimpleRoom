package tech.xclz.utils

import io.ktor.utils.io.*
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.charset.Charset

val logger = KotlinLogging.logger("tech.xclz")

suspend inline fun ByteWriteChannel.writeUInt(b: UInt) = writeInt(b.toInt())
suspend inline fun ByteWriteChannel.writeUByte(b: UByte) = writeByte(b.toByte())
suspend inline fun ByteWriteChannel.writeUShort(s: UShort) = writeShort(s.toShort())
suspend inline fun ByteWriteChannel.writeString(s: String) {
    writeInt(s.length)
    writeFully(s.toByteArray())
}

suspend inline fun ByteReadChannel.readUByte(): UByte = readByte().toUByte()
suspend inline fun ByteReadChannel.readUShort(): UShort = readShort().toUShort()
suspend inline fun ByteReadChannel.readUInt(): UInt = readInt().toUInt()
suspend inline fun ByteReadChannel.readBytes(size: Int): ByteArray {
    val buffer = ByteBuffer.allocate(size).also {
        readFully(it)
        it.flip()
    }
    return ByteArray(size) { buffer.get() }
}

suspend inline fun ByteReadChannel.readString(size: Int, charset: Charset = Charsets.UTF_8): String =
    this.readBytes(size).toString(charset)

suspend inline fun ByteReadChannel.readString(charset: Charset = Charsets.UTF_8): String =
    this.readString(this.readInt(), charset)
