package tech.xclz

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import tech.xclz.core.*
import tech.xclz.utils.*
import kotlin.test.Test
import kotlin.test.assertEquals

const val HOST_NAME = "127.0.0.1"
const val PORT = 9999
val logger = KotlinLogging.logger {}

class Client(val deviceID: String, socket: Socket) {
    val reader = socket.openReadChannel()
    val writer = socket.openWriteChannel()
    val writeMutex = Mutex()
    val readMutex = Mutex()
    val channels = mutableMapOf<UShort, Channel<Any>>()

    private var askId: UShort = 0u
        get() {
            if (field < 0xFFFFu) field++
            else field = 1u
            //TODO 优化askId逻辑
            channels[field] = Channel()
            return field
        }

    private suspend fun sendCommand(type: CommandType, body: (suspend (ByteWriteChannel) -> Unit)? = null): Any {
        val curAskId = askId
        logger.debug { "[$deviceID] 客户端开始发送命令 type=$type askId=$curAskId" }

        withWriteLock {
            writer.writeCommandType(type)
            writer.writeUShort(curAskId)
            body?.invoke(it)
        }
        logger.debug { "[$deviceID] 客户端已发送完命令，等待回复 type=$type askId=$curAskId" }

        val channel = channels[curAskId] ?: TODO("发生错误，没有找到askId对应的channel")
        val result = channel.receive()
        logger.debug { "[$deviceID] 客户端收到命令结果 type=$type askId=$curAskId result=$result" }
        channel.close()
        channels.remove(curAskId)
        return result
    }

    suspend fun getVersion(): Short {
        return sendCommand(CommandType.GetVersion) as Short
    }

    suspend fun bindDevice(): Boolean {
        return sendCommand(CommandType.BindDevice) {
            writer.writeString(deviceID)
        } as Boolean
    }

    suspend fun createRoom(): RoomID {
        return sendCommand(CommandType.CreateRoom) as RoomID
    }

    suspend fun joinRoom(roomID: RoomID): Int {
        return sendCommand(CommandType.JoinRoom) {
            writer.writeRoomID(roomID)
        } as Int
    }

    suspend fun sendMessage(message: SyncMessage) {
        logger.debug { "[$deviceID] 客户端开始发送消息 $message" }
        withWriteLock {
            writer.writeCommandType(CommandType.SendMessage)
            writer.writeMessageType(message.type)
            writer.writeLong(message.time)

            when (message.type) {
                MessageType.PlayNote -> {
                    writer.writeUByte((message as PlayNoteMessage).note)
                }
                MessageType.StopNote -> {
                    writer.writeUByte((message as StopNoteMessage).note)
                }
            }
        }
        logger.debug { "[$deviceID] 客户端已发送消息: $message" }
    }

    suspend inline fun withWriteLock(action: (ByteWriteChannel) -> Unit) =
        writeMutex.withLock {
            action(writer)
            writer.flush()
        }

    suspend inline fun withReadLock(action: (ByteReadChannel) -> Unit) = readMutex.withLock { action(reader) }
}

internal fun CoroutineScope.startClient(deviceID: String, action: (suspend (Client) -> Unit)? = null) = launch {
    val selector = ActorSelectorManager(Dispatchers.IO)
    val socket = aSocket(selector).tcp().connect(HOST_NAME, PORT)
    val client = Client(deviceID, socket)

    launch { action?.invoke(client) }
    while (true) {
        logger.debug { "[$deviceID] 客户端读取服务器回复中..." }
        val cmdType = client.reader.readCommandType() ?: TODO("错误的类型")
        if (cmdType == CommandType.SendMessage) {
            logger.debug { "[$deviceID] 客户端读取正在读取消息..." }
            client.withReadLock { reader ->
                val type = reader.readMessageType()
                    ?: TODO("如果type为空怎么办")
                val time = reader.readLong()

                val message = when (type) {
                    MessageType.PlayNote -> {
                        val note = reader.readUByte()
                        PlayNoteMessage(time, note)
                    }
                    MessageType.StopNote -> {
                        val note = reader.readUByte()
                        StopNoteMessage(time, note)
                    }
                }
                logger.info { "[$deviceID] 客户端收到服务器发来的消息：$message" }
            }
            continue
        }

        val askId = client.reader.readUShort()
        logger.debug { "[$deviceID] 客户端读取到: type=$cmdType askId=$askId" }

        val channel = client.channels[askId] ?: TODO("发生错误，没有找到askId对应的channel")

        val result: Any = when (cmdType) {
            CommandType.GetVersion -> client.reader.readShort()
            CommandType.CreateRoom -> client.reader.readRoomID()
            CommandType.JoinRoom -> client.reader.readInt()
            CommandType.LeaveRoom -> TODO()
            CommandType.BindDevice -> client.reader.readBoolean()
            else -> TODO("奇怪了，不应该进入else分支的")
        }
        channel.send(result)
    }
}

@OptIn(ObsoleteCoroutinesApi::class)
class ApplicationTest {
    @Test
    fun testAll() {
        val server = RoomServer(hostname = HOST_NAME, port = PORT)

        runBlocking {
            val serverJob = launch { server.start() }

            delay(100)

            runBlocking {
                val roomIDChannel = BroadcastChannel<RoomID>(Channel.BUFFERED)
                val clientJobs = mutableListOf<Job>()
                clientJobs.add(startClient("0000") { client ->
                    //这个是房主
                    val version = client.getVersion()
                    assertEquals(SERVER_VERSION, version)
                    val result = client.bindDevice()
                    logger.info { "[${client.deviceID}] 已成功绑定device $result" }
                    val roomID = client.createRoom()
                    logger.info { "[${client.deviceID}] 创建房间完成，广播roomID" }
                    roomIDChannel.send(roomID)
                    roomIDChannel.close()
                    logger.info { "[${client.deviceID}] 关闭广播通道，开始发送音符消息" }
                    delay(200) //等待其他客户端加入房间
                    var message = PlayNoteMessage(System.currentTimeMillis(), 33u)
                    client.sendMessage(message)
                    logger.info { "[${client.deviceID}] 已发送33音符" }
                    delay(200)
                    message = PlayNoteMessage(System.currentTimeMillis(), 35u)
                    client.sendMessage(message)
                    logger.info { "[${client.deviceID}] 已发送35音符" }
                })
                repeat(3) {
                    val deviceID = "000${it + 1}"
                    val clientJob = startClient(deviceID) { client ->
                        val version = client.getVersion()
                        assertEquals(SERVER_VERSION, version)
                        val result = client.bindDevice()
                        logger.info { "[${client.deviceID}] 已成功绑定device $result" }
                        val roomID = roomIDChannel.openSubscription().receive()
                        logger.info { "[$deviceID] 获取到roomID，开始加入房间" }
                        val num = client.joinRoom(roomID)
                        logger.info { "[$deviceID] 加入房间成功，当前房间人数: $num" }
                    }
                    clientJobs.add(clientJob)
                }

                delay(10000) //强制等待10s  然后结束所有连接
                logger.info { "已强制等待10s，开始结束所有客户端" }
                for (clientJob in clientJobs) {
                    clientJob.cancel()
                    clientJob.join()
                }
            }

            serverJob.cancel()
            serverJob.join()
        }
    }
}