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
    var state = state("*")  // current state

    private val states = mutableMapOf<String, State>()

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
