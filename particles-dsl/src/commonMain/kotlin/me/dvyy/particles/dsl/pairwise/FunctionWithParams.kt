package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.Parameter
import me.dvyy.particles.dsl.glsl.GLSLParameter

class FunctionWithParams(
    val uniformPrefix: String,
    val function: PairwiseFunction,
    params: Map<String, Parameter<*>>,
) {
    val parameters: List<Pair<GLSLParameter, Parameter<*>>> = function.parameters.map {
        it to (params[it.name] ?: error("Parameter ${it.name} not set"))
    }

    fun renderFunctionCall() = """
        ${function.name}(dist${
        parameters.joinToString(prefix = ", ", separator = ", ") { (glsl, param) ->
            when (param) {
                is Parameter.Value<*> -> param.value.toString()
                is Parameter.FromParams<*> -> "${glsl.name}_$uniformPrefix"
            }
        }
    })
        """.trimIndent()
}
