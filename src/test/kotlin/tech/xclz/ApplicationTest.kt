package tech.xclz

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.xclz.core.CommandType
import tech.xclz.core.RoomIDManager
import tech.xclz.core.readCommandType
import tech.xclz.core.writeCommandType
import tech.xclz.utils.readUShort
import tech.xclz.utils.writeString
import tech.xclz.utils.writeUShort
import kotlin.test.Test
import kotlin.test.assertEquals

const val HOST_NAME = "127.0.0.1"
const val PORT = 9999

class ApplicationTest {
    @Test
    fun testAll() {
        suspend fun client(deviceID: String) {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(HOST_NAME, PORT)

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            sendChannel.writeCommandType(CommandType.GetVersion)
            sendChannel.writeUShort(1u)
            sendChannel.writeString(deviceID)

            val type = receiveChannel.readCommandType()
            val askId = receiveChannel.readUShort()
            val version = receiveChannel.readUShort()
            println("$deviceID type=$type askId=$askId version=$version")
            assertEquals(SERVER_VERSION, version.toInt())
        }

        val server = RoomServer(hostname = HOST_NAME, port = PORT)

        runBlocking {
            val serverJob = launch {
                server.start()
            }
            delay(100)

            runBlocking {
                repeat(3) {
                    launch {
                        client(RoomIDManager.getRoomID().toString())
                    }
                }
            }

            serverJob.cancel()
            serverJob.join()
        }
    }
}