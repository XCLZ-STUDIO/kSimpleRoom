package tech.xclz

const val SERVER_VERSION = 1

enum class PlayerState : Statizable {
    NotInRoom,
    Manager,
    Member,
    ManagerIDLE,
    MemberIDLE,
    ;

    override fun state() = name
}

enum class PlayerAction : Actionizable {
    connect,
    create,
    join,
    disconnect,
    leave,
    ;

    override fun action() = name
}