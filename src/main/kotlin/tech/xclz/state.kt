package tech.xclz

class State(val name: String, private val machine: StateMachine) {
    private val transitionMap = mutableMapOf<String, State>()

    infix fun String.goto(destName: String) {
        val action = this
        if (action in transitionMap) {
            throw IllegalArgumentException("Duplicate Action: $name, $action")
        }
        transitionMap[action] = machine.state(destName)
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

    infix fun String.by(block: State.() -> Unit) {
        val startName = this
        state(startName).block()
    }
}

fun buildStateMachine(block: StateMachine.() -> Unit) = StateMachine().apply { block() }
