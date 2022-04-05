package tech.xclz.korom.protocol

abstract class SyncMessage(val frameID: UInt, val type: MessageType, val time: Long)

class PlayNoteMessage(frameID: UInt, time: Long, val note: UByte) : SyncMessage(frameID, MessageType.PlayNote, time) {
    override fun toString(): String {
        return "tech.xclz.core.PlayNoteMessage(time=$time, note=$note)"
    }
}

class StopNoteMessage(frameID: UInt, time: Long, val note: UByte) : SyncMessage(frameID, MessageType.PlayNote, time) {
    override fun toString(): String {
        return "tech.xclz.core.StopNoteMessage(time=$time, note=$note)"
    }
}