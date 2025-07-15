package me.dvyy.particles.compute.forces.builders

import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.KslUniformScalar
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.dsl.pairwise.ParticlePair

data class FunctionParameter<T>(val name: String, val serializer: KSerializer<T>) {
    fun uniformNameFor(pair: ParticlePair) = "${name}_${pair.hash}"

    fun asUniform(builder: KslProgram, pair: ParticlePair): KslUniformScalar<*> = builder.run {
        val name = uniformNameFor(pair)
        when (serializer) {
            Float.serializer() -> uniformFloat1(name)
            Int.serializer() -> uniformInt1(name)
            else -> error("Unsupported field type $serializer")
        }
    }
}
