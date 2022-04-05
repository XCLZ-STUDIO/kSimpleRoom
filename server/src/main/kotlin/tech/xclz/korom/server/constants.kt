package tech.xclz.korom.server

import mu.KotlinLogging

const val SERVER_VERSION: Short = 1
val logger = KotlinLogging.logger("tech.xclz.korom.server")

enum class PlayerState : Statizable {
    Member,
    Manager,
    NotInRoom,
    MemberIDLE,
    ManagerIDLE;

    override fun state() = name
}

@Suppress("EnumEntryName")
enum class PlayerAction : Actionizable {
    join,
    leave,
    create,
    connect,
    disconnect;

    override fun action() = name
}
