package tech.xclz.korom.protocol

import io.ktor.utils.io.*

data class DeviceID(private val value: String) {
    override fun toString(): String = value
}

suspend inline fun ByteReadChannel.readDeviceID() = DeviceID(readString())