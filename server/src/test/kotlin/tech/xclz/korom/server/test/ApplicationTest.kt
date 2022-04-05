package tech.xclz.korom.server.test

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import tech.xclz.korom.client.startClient
import tech.xclz.korom.protocol.PlayNoteMessage
import tech.xclz.korom.protocol.RoomID
import tech.xclz.korom.server.RoomServer
import tech.xclz.korom.server.SERVER_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals

const val HOST_NAME = "127.0.0.1"
const val PORT = 9999
val logger = KotlinLogging.logger {}

@OptIn(ObsoleteCoroutinesApi::class)
class ApplicationTest {
    @Test
    fun testAll() {
        val server = RoomServer(hostname = HOST_NAME, port = PORT)

        runBlocking {
            withContext(Dispatchers.IO) {
                val serverJob = launch { server.start() }

                delay(100)

                runBlocking {
                    val roomIDChannel = BroadcastChannel<RoomID>(Channel.BUFFERED)
                    val clientJobs = mutableListOf<Job>()
                    clientJobs.add(startClient(HOST_NAME, PORT, "0000") { client ->
                        logger.debug { "[0000] 客户端已经与服务器建立连接" }
                        //这个是房主
                        val version = client.getVersion()
                        assertEquals(SERVER_VERSION, version)
                        val result = client.bindDevice()
                        logger.info { "[${client.deviceID}] 已成功绑定device $result" }
                        val timeBeforeCreateRoom = System.currentTimeMillis()
                        val roomID = client.createRoom()
                        val timeAfterCreateRoom = System.currentTimeMillis()
                        val startTime = (timeAfterCreateRoom + timeBeforeCreateRoom) / 2 //TODO 考虑两个Long相加的溢出问题
                        client.startTime = startTime
                        logger.info { "[${client.deviceID}] 创建房间完成 $timeBeforeCreateRoom $timeAfterCreateRoom 起始时间：${client.startTime}，广播roomID" }
                        roomIDChannel.send(roomID)
                        roomIDChannel.close()
                        logger.info { "[${client.deviceID}] 关闭广播通道，开始发送音符消息" }
                        delay(200) //等待其他客户端加入房间
                        var playNoteTime = System.currentTimeMillis()
                        var message = PlayNoteMessage(1u, playNoteTime - startTime, 33u)
                        client.sendMessage(message)
                        logger.info { "[${client.deviceID}] 已发送33音符 本地时间: $playNoteTime 相对时间：${message.time}" }
                        delay(500)
                        playNoteTime = System.currentTimeMillis()
                        message = PlayNoteMessage(2u, playNoteTime - startTime, 35u)
                        client.sendMessage(message)
                        logger.info { "[${client.deviceID}] 已发送35音符 本地时间：$playNoteTime 相对时间：${message.time}" }
                    })
                    repeat(3) {
                        val deviceID = "000${it + 1}"
                        val clientJob = startClient(HOST_NAME, PORT, deviceID) { client ->
                            val version = client.getVersion()
                            assertEquals(SERVER_VERSION, version)
                            val result = client.bindDevice()
                            logger.info { "[${client.deviceID}] 已成功绑定device $result" }
                            val roomID = roomIDChannel.openSubscription().receive()
                            logger.info { "[$deviceID] 获取到roomID，开始加入房间" }
                            val timeBeforeJoinRoom = System.currentTimeMillis()
                            val joinRoomTime = client.joinRoom(roomID)
                            val timeAfterJoinRoom = System.currentTimeMillis()
                            client.startTime =
                                (timeAfterJoinRoom + timeBeforeJoinRoom) / 2 - joinRoomTime //TODO 考虑两个Long相加的溢出问题
                            logger.info { "[$deviceID] 加入房间成功, 相对时间: $joinRoomTime 房间起始时间: ${client.startTime} " }
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
}