package tech.xclz.korom.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import tech.xclz.korom.protocol.*

val logger = KotlinLogging.logger {}

class Client(val deviceID: String, socket: Socket) {
    var startTime: Long = 0
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

    suspend fun joinRoom(roomID: RoomID): Long {
        return sendCommand(CommandType.JoinRoom) {
            writer.writeRoomID(roomID)
        } as Long
    }

    suspend fun sendMessage(message: SyncMessage) {
        logger.debug { "[$deviceID] 客户端开始发送消息 $message" }
        withWriteLock {
            writer.writeCommandType(CommandType.SendMessage)
            writer.writeUInt(message.frameID)
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

fun CoroutineScope.startClient(host: String, port: Int, deviceID: String, action: (suspend (Client) -> Unit)? = null) =
    launch {
        val selector = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selector).tcp().connect(host, port)
        logger.debug { "[$deviceID] 客户端已经与服务器建立连接" }
        val client = Client(deviceID, socket)

        launch { action?.invoke(client) }
        while (true) {
            logger.debug { "[$deviceID] 客户端读取服务器回复中..." }
            val cmdType = client.reader.readCommandType() ?: TODO("错误的类型")
            if (cmdType == CommandType.SendMessage) {
                logger.debug { "[$deviceID] 客户端读取正在读取消息..." }
                client.withReadLock { reader ->
                    val frameID = reader.readUInt()
                    val type = reader.readMessageType()
                        ?: TODO("如果type为空怎么办")
                    val time = reader.readLong()

                    val message = when (type) {
                        MessageType.PlayNote -> {
                            val note = reader.readUByte()
                            PlayNoteMessage(frameID, time, note)
                        }
                        MessageType.StopNote -> {
                            val note = reader.readUByte()
                            StopNoteMessage(frameID, time, note)
                        }
                    }
                    val messageTime = client.startTime + time
                    logger.info { "[$deviceID] 客户端收到服务器发来的消息：$message 播放音符的时间为：$messageTime" }
                }
                continue
            }

            val askId = client.reader.readUShort()
            logger.debug { "[$deviceID] 客户端读取到: type=$cmdType askId=$askId" }

            val channel = client.channels[askId] ?: TODO("发生错误，没有找到askId对应的channel")

            val result: Any = when (cmdType) {
                CommandType.GetVersion -> client.reader.readShort()
                CommandType.CreateRoom -> client.reader.readRoomID()
                CommandType.JoinRoom -> client.reader.readLong()
                CommandType.LeaveRoom -> TODO()
                CommandType.BindDevice -> client.reader.readBoolean()
                else -> TODO("奇怪了，不应该进入else分支的: $cmdType")
            }
            channel.send(result)
        }
    }