package tech.xclz

import kotlin.test.Test
import kotlin.test.assertEquals

class RoomIDManagerTest {
    @Test
    fun testRoomID() {
        assertEquals("0000", RoomID(0).toString())
        assertEquals("0001", RoomID(1).toString())
        assertEquals("0002", RoomID(2).toString())
        assertEquals("0003", RoomID(3).toString())
        assertEquals("0004", RoomID(4).toString())
        assertEquals("000A", RoomID(10).toString())
        assertEquals("000B", RoomID(11).toString())
        assertEquals("7CFZ", RoomID(342719).toString())
        assertEquals("7CIO", RoomID(342816).toString())
        assertEquals("XCLZ", RoomID(1555991).toString())
    }

    @Test
    fun testRoomIDManager() {
        val roomIDNum = 100

        val roomIDSet = mutableSetOf<RoomID>()

        (0 until roomIDNum).forEach { _ ->
            roomIDSet.add(RoomIDManager.getRoomID())
        }

        println(roomIDSet)
        assertEquals(roomIDNum, roomIDSet.count())
    }
}