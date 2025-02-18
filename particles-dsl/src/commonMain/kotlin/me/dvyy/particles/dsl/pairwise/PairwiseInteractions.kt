package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.Parameter

data class UniformParameter<T>(
    val name: String,
    val type: String,
    val hash: String? = null,
    val parameter: Parameter.FromParams<T>,
    val precision : Int = 0,
    val range: ClosedFloatingPointRange<Double> = 0.0..100.0,
) {
    val uniformName = if (hash != null) "${name}_$hash" else name
}

class PairwiseInteractions(
    val type: ParticlePair,
    val functions: List<FunctionWithParams>,
) {
    val uniforms: List<UniformParameter<*>> = functions.flatMap {
        it.parameters.mapNotNull { (glsl, param) ->
            if (param !is Parameter.FromParams<*>) return@mapNotNull null
            UniformParameter(
                glsl.name,
                glsl.type,
                type.hash.toString(),
                param,
                range = param.min..param.max
            )
        }
    }

    fun uniformStrings(): String = functions.joinToString("\n") { function ->
        uniforms.joinToString("\n") { uniform ->
            "uniform ${uniform.type} ${uniform.uniformName};"
        }
    }
}
