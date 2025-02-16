package me.dvyy.particles.dsl.pairwise

import kotlinx.serialization.KSerializer
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

    companion object {
        fun <T> from(
            serializer: KSerializer<T>,
            type: String,
            name: String,
            path: String,
            default: T,
            precision : Int = 1,
            range: ClosedFloatingPointRange<Double> = 0.0..100.0,
        ): UniformParameter<T> {
            return UniformParameter(
                name,
                type,
                null,
                Parameter.FromParams<T>(path, serializer, default),
                precision,
                range
            )
        }
    }
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
