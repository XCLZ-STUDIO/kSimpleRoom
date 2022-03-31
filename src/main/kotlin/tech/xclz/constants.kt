package tech.xclz

const val SERVER_VERSION = 1

enum class PlayerState(name: String) : Statizable {
    NotInRoom("NotInRoom"),
    Manager("Manager"),
    Member("Member"),
    ManagerIDLE("ManagerIDLE"),
    MemberIDLE("MemberIDLE"),
    ;

    override fun state() = this.name
}

enum class PlayerAction(name: String) : Actionizable {
    connect("connect"),
    create("create"),
    join("joinRoom"),
    disconnect("disconnect"),
    leave("leave"),
    ;

    override fun action() = this.name
}