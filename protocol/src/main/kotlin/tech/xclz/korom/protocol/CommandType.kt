package tech.xclz.korom.protocol

import io.ktor.utils.io.*

enum class CommandType(val value: UShort) {
    GetVersion(0x0001u),
    CreateRoom(0x0002u),
    JoinRoom(0x0003u),
    LeaveRoom(0x0004u),
    SendMessage(0x0005u),
    BindDevice(0x0006u);

    companion object {
        fun from(value: UShort) = values().firstOrNull { it.value == value }
    }
}

suspend inline fun ByteReadChannel.readCommandType(): CommandType? = CommandType.from(readUShort())
suspend inline fun ByteWriteChannel.writeCommandType(type: CommandType) = writeUShort(type.value)
