package tech.xclz.korom.protocol

import io.ktor.utils.io.*

enum class MessageType(val value: UShort) {
    PlayNote(0x0001u),
    StopNote(0x0002u);

    companion object {
        fun from(value: UShort) = values().firstOrNull { it.value == value }
    }
}

suspend inline fun ByteReadChannel.readMessageType(): MessageType? = MessageType.from(readUShort())
suspend inline fun ByteWriteChannel.writeMessageType(type: MessageType) = writeUShort(type.value)
