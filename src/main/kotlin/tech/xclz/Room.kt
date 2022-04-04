package tech.xclz

import tech.xclz.core.RoomID

class Room(val id: RoomID) {
    val startTime = System.currentTimeMillis()
    val players = mutableListOf<Player>()
    val time: Long
        get() = System.currentTimeMillis() - startTime

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun removePlayer(player: Player) {
        players.remove(player)
    }
}