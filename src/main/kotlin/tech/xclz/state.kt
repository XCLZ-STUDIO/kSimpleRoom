package tech.xclz

interface Statizable {
    fun state(): String
}

interface Actionizable {
    fun action(): String
}

class State(val name: String, private val machine: StateMachine) {
    private val transitionMap = mutableMapOf<String, State>()

    infix fun <T> T.goto(destState: T) where T : Statizable {
        this@goto.state() goto destState.state()
    }

    infix fun String.goto(destState: String) {
        val action = this
        if (action in transitionMap) {
            throw IllegalArgumentException("Duplicate Action: $name, $action")
        }
        transitionMap[action] = machine.state(destState)
    }

    fun on(action: String): State {
        return transitionMap[action] ?: throw IllegalArgumentException("No such action: $action")
    }

    override fun equals(other: Any?): Boolean {
        return (other is String && other == name) || (other is State && other.name == name)
    }

    override fun toString(): String {
        return "State(name=\"$name\")"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class StateMachine {
    val initState by lazy { state("*") }  // current state

    private val states = mutableMapOf<String, State>()

    fun state(name: String) = states[name] ?: State(name, this).also { states[name] = it }

    infix fun <T> T.by(block: State.() -> Unit) where T : Actionizable {
        this@by.action() by block
    }

    infix fun String.by(block: State.() -> Unit) {
        val startName = this
        state(startName).block()
    }
}

fun buildStateMachine(block: StateMachine.() -> Unit) = StateMachine().apply { block() }
