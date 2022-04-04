package tech.xclz.core

import io.ktor.utils.io.*
import java.util.*
import kotlin.math.pow

const val ROOM_ID_CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val ROOM_ID_LENGTH = 4

data class RoomID(val value: Int) {
    private val idString: String = idToString(value)

    override fun toString(): String = idString

    companion object {
        private fun idToString(index: Int): String {
            var num = index
            val tmpID = StringBuilder()
            repeat(ROOM_ID_LENGTH) {
                tmpID.append(ROOM_ID_CHAR_SET[num % ROOM_ID_CHAR_SET.length])
                num /= ROOM_ID_CHAR_SET.length
            }
            return tmpID.reverse().toString()
        }
    }
}

suspend fun ByteReadChannel.readRoomID(): RoomID = RoomID(readInt())
suspend fun ByteWriteChannel.writeRoomID(roomID: RoomID) = writeInt(roomID.value)


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