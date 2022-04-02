package tech.xclz

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.pow

const val ROOM_ID_CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val ROOM_ID_LENGTH = 4

class RoomID(private val id: Int) {
    private val idString: String = generateRoomID(id)

    constructor(idString: String) : this(idString.toInt())

    override fun toString(): String {
        return idString
    }

    private fun generateRoomID(index: Int): String {
        var num = index
        val tmpID = StringBuilder()
        (1..ROOM_ID_LENGTH).forEach { _ ->
            tmpID.append(ROOM_ID_CHAR_SET[num % ROOM_ID_CHAR_SET.length])
            num /= ROOM_ID_CHAR_SET.length
        }
        return tmpID.reverse().toString()
    }
}


object RoomIDManager {
    private val roomIDs = ArrayDeque<RoomID>()

    init {
        val totalRoomIDNum = ROOM_ID_CHAR_SET.length.toDouble().pow(ROOM_ID_LENGTH).toInt()

        (0..totalRoomIDNum).forEach {
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