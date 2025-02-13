package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.Parameter
import me.dvyy.particles.dsl.glsl.GLSLParameter

data class UniformParameter(
    val name: String,
    val type: String,
    val hash: String,
    val parameter: Parameter.FromParams<*>
) {
    val uniformName = "${name}_$hash"
}
class PairwiseInteractions(
    val type: ParticlePair,
    val functions: List<FunctionWithParams>,
) {
    val uniforms: List<UniformParameter> = functions.flatMap {
        it.parameters.mapNotNull { (glsl, param) ->
            if(param !is Parameter.FromParams<*>) return@mapNotNull null
            UniformParameter(
                glsl.name,
                glsl.type,
                type.hash,
                param
            )
        }
    }

    fun uniformStrings(): String = functions.joinToString("\n") { function ->
        uniforms.joinToString("\n") { uniform ->
            "uniform ${uniform.type} ${uniform.uniformName};"
        }
    }
}
