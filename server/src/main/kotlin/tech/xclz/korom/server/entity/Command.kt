package tech.xclz.korom.server.entity

import tech.xclz.korom.protocol.CommandType
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class Command(
    val type: CommandType,
    val askId: UShort,
    val function: KFunction<*>,
    val arguments: Map<KParameter, Any>
) {
    override fun toString(): String {
        return "tech.xclz.core.Command(type=$type, askId=$askId)"
    }
}