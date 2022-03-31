package tech.xclz

class Room(val code: RoomID) {
    private val startTime = System.currentTimeMillis()
    private val players = mutableListOf<Player>()

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun removePlayer(player: Player) {
        players.remove(player)
    }
}