package tech.xclz

import RoomServer
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.xclz.plugins.*
import writeString
import writeUShort

const val HOST_NAME = "127.0.0.1"
const val PORT = 9999

class ApplicationTest {
    @Test
    fun testClient() {

        suspend fun client() {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(HOST_NAME, PORT)

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            sendChannel.writeUShort(CommandID.Version.id)
            sendChannel.writeString("abcdefg")
        }

        val server = RoomServer(hostname = HOST_NAME, port = PORT)

        runBlocking {
            launch {
                server.start()
            }
            launch {
                if (server.readyToConnect) {
                    client()
                }
            }
        }
    }
}