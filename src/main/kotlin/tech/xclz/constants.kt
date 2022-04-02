package tech.xclz

import tech.xclz.core.Actionizable
import tech.xclz.core.Statizable

const val SERVER_VERSION = 1

enum class PlayerState : Statizable {
    NotInRoom,
    Manager,
    Member,
    ManagerIDLE,
    MemberIDLE;

    override fun state() = name
}

@Suppress("EnumEntryName")
enum class PlayerAction : Actionizable {
    connect,
    create,
    join,
    disconnect,
    leave;

    override fun action() = name
}
