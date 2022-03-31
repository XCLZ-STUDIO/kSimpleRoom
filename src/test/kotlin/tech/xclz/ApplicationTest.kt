package tech.xclz

import RoomServer
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

const val HOST_NAME = "127.0.0.1"
const val PORT = 9999

class ApplicationTest {
    @Test
    fun testAll() {
        suspend fun client() {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(HOST_NAME, PORT)

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            sendChannel.writeUShort(CommandID.Version.id)
            sendChannel.writeString("abcdefg")
            val result = receiveChannel.readInt()
            assertEquals(SERVER_VERSION, result)
        }

        val server = RoomServer(hostname = HOST_NAME, port = PORT)

        runBlocking {
            val serverJob = launch {
                server.start()
            }
            delay(100)
            client()

            serverJob.cancel()
            serverJob.join()
        }
    }
}