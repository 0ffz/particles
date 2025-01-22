package me.dvyy.particles.dsl

import org.intellij.lang.annotations.Language

data class InteractionParameter(
    val name: String,
    val type: String,
)

sealed interface ParameterValue {
    data class Value(val value: Any) : ParameterValue
}

class FunctionWithParameters(
    internal val function: PairwiseFunction,
) {
    internal val setParams = mutableMapOf<InteractionParameter, ParameterValue>()

    infix fun InteractionParameter.set(value: Any) {
        setParams[this] = ParameterValue.Value(value)
    }

    fun getParameters(): List<Any> {
        return function.parameters.map {
            val param = setParams[it] ?: error("Parameter ${it.name} not set")
            getParameter(param)
        }
    }

    fun getParameter(param: ParameterValue): Any = when (param) {
        is ParameterValue.Value -> param.value
    }
}

abstract class PairwiseFunction(
    val name: String,
    @Language(
        "glsl", prefix = """
    float sigma;
    float epsilon;
    float func(float dist) {
    """, suffix = "}"
    )
    val body: String,
) {
    // TODO immutability - split into builder
    val parameters = mutableListOf<InteractionParameter>()
    fun parameter(type: String, name: String) = InteractionParameter(name, type).also { parameters.add(it) }

    fun render() = """
        float $name(float dist${
        if (parameters.isEmpty()) "" else parameters.joinToString(
            prefix = ", ",
            separator = ", "
        ) { "${it.type} ${it.name}" }
    }) {
            $body
        }
    """.trimIndent()

//    fun parametersFromConfig(parameters: YamlConfig, interaction: Pair<ParticleType, ParticleType>): List<Any> {
//        val interactionRoot = "${interaction.first.name}-${interaction.second.name}"
//        val interactionRootReversed = "${interaction.second.name}-${interaction.first.name}"
//        return this.parameters.map { param ->
//            val key = "$name.${param.name}"
//            val value = parameters.propertyOrNull("$interactionRoot.$key$")
//                ?: parameters.propertyOrNull("$interactionRootReversed.$key$")
//                ?: parameters.propertyOrNull("default.$key")
//                ?: error("Parameter $key not found in configuration")
//            value.getString()
//        }
//    }
}
