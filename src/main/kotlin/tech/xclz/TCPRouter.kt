package tech.xclz

import tech.xclz.core.*
import tech.xclz.utils.logger
import tech.xclz.utils.writeUByte


@Suppress("unused", "RedundantSuspendModifier", "UNUSED_PARAMETER")
object TCPRouter : Router() {
    @Route(CommandType.GetVersion)
    suspend fun version(session: ClientSession): Short {
        return SERVER_VERSION
    }

    @Route(CommandType.BindDevice)
    suspend fun bindDevice(session: ClientSession, deviceID: DeviceID): Boolean {
        logger.debug { "[$deviceID] 服务器正在执行绑定设备" }
        if (deviceID in session.server) {
            logger.error { "[$deviceID] 设备已在房间里，出现错误" }
            TODO("Already in room, ready to recover the connection")
        }
        session.connect(deviceID) //bind player to session
        logger.debug { "[$deviceID] 服务器执行绑定设备完成" }
        return true
    }

    @Route(CommandType.CreateRoom)
    suspend fun createRoom(session: ClientSession): RoomID {
        logger.debug { "[${session.player?.deviceId}] 服务器正在执行创建房间命令" }
        val roomID = session.createRoom()?.id
        logger.debug { "[${session.player?.deviceId}] roomID=$roomID" }
        return roomID ?: TODO("创建失败怎么办")
    }

    @Route(CommandType.JoinRoom)
    suspend fun joinRoom(session: ClientSession, roomID: RoomID): Int {
        logger.debug { "[${session.player?.deviceId}] 服务器正在执行加入房间命令" }
        val room = session.server.room(roomID)
        session.joinRoom(room)
        return room.players.size
    }

    @Route(CommandType.LeaveRoom)
    suspend fun leaveRoom(session: ClientSession): Boolean {
        session.leaveRoom()
        return true
    }

    private suspend fun writeSyncMessage(session: ClientSession, message: SyncMessage) {
        session.withSendLock { sendChannel ->
            sendChannel.writeCommandType(CommandType.SendMessage)
            sendChannel.writeMessageType(message.type)
            sendChannel.writeLong(message.time)

            when (message.type) {
                MessageType.PlayNote -> sendChannel.writeUByte((message as PlayNoteMessage).note)
                MessageType.StopNote -> sendChannel.writeUByte((message as StopNoteMessage).note)
            }
        }
    }

    override suspend fun handleMessage(session: ClientSession, message: SyncMessage) {
        logger.debug("服务器开始处理消息$message，当前房间: ${session.room}")
        val room = session.room ?: TODO("如果没有房间")
        for (player in room.players) {
            val session2 = player.session ?: TODO("如果没有session")
            if (session != session2) {  //筛选掉消息的来源用户
                writeSyncMessage(session2, message) //向其他用户广播该消息
            }
        }
    }
}
