package tech.xclz

class State(val name: String, val machine: StateMachine) {
    val transitionMap = mutableMapOf<String, State>()

    infix fun String.goto(destName: String) {
        val action = this
        if (action in transitionMap) {
            throw IllegalArgumentException("Duplicate Action: $name, $action")
        }
        transitionMap[action] = machine.state(destName)
    }

    override fun toString(): String {
        return "State(name=\"$name\")"
    }
}

class StateMachine {
    private val states = mutableMapOf<String, State>()
    var state = state("*")  // current state

    fun state(name: String) = states[name] ?: State(name, this).also { states[name] = it }

    fun on(action: String) {
        state.transitionMap[action]?.apply {
            state = this
        } ?: throw IllegalArgumentException("No such action: $action")
    }

    infix fun String.by(block: State.() -> Unit) {
        val startName = this
        state(startName).block()
    }
}

fun buildStateMachine(block: StateMachine.() -> Unit) = StateMachine().apply { block() }

fun main() {
    val machine = buildStateMachine {
        "*" by {
            "建立连接" goto "未加入房间"
        }

        "未加入房间" by {
            "创建房间" goto "房主"
            "加入房间" goto "成员"
            "断开连接" goto "*"
        }

        "未加入房间" by {
            "房主" goto "创建房间"
            "成员" goto "加入房间"
            "*" goto "断开连接"
        }

        "房主" by {
            "退出房间" goto "未加入房间"
            "断开连接" goto "房主待恢复连接"
        }

        "成员" by {
            "退出房间" goto "未加入房间"
            "断开连接" goto "成员待恢复连接"
        }
    }
    println(machine.state)
    machine.on("建立连接")
    println(machine.state)
    machine.on("创建房间")
    println(machine.state)
    machine.on("退出房间")
    println(machine.state)
    machine.on("加入房间")
    println(machine.state)
    machine.on("断开连接")
    println(machine.state)
}
