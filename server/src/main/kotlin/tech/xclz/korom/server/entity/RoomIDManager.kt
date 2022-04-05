package tech.xclz.korom.server.entity

import tech.xclz.korom.protocol.ROOM_ID_CHAR_SET
import tech.xclz.korom.protocol.ROOM_ID_LENGTH
import tech.xclz.korom.protocol.RoomID
import java.util.*
import kotlin.math.pow

object RoomIDManager {
    private val roomIDs = LinkedList<RoomID>()

    init {
        val totalRoomIDNum = ROOM_ID_CHAR_SET.length.toDouble().pow(ROOM_ID_LENGTH).toInt()

        repeat(totalRoomIDNum) {
            roomIDs.add(RoomID(it))
        }
        roomIDs.shuffle()
    }

    fun getRoomID(): RoomID {
        return roomIDs.removeFirst()
    }

    fun returnRoomID(roomID: RoomID) {
        roomIDs.add(roomID)
    }
}