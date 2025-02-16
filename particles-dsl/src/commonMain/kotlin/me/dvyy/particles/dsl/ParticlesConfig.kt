package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.dvyy.particles.dsl.pairwise.*

@Serializable
data class ParticlesConfig(
    val application: ApplicationConfiguration = ApplicationConfiguration(),
    val particles: Map<String, Particle> = mapOf(),
    val interactions: Map<String, Map<String, Map<String, Parameter<String>>>> = mapOf(),
) {
    @Transient
    val functions: List<PairwiseFunction> = listOf(LennardJones)

    @Transient
    private var highestId = 0u

    @Transient
    val particleIds: Map<String, ParticleId> = particles.mapValues { ParticleId(highestId++) }

    @Transient
    val pairwiseInteraction: List<PairwiseInteractions> = interactions.map { (name, interactions) ->
        val (first, second) = name.split("-")
        val pair = ParticlePair(particleIds[first]!!, particleIds[second]!!)
        PairwiseInteractions(
            pair,
            interactions.map { (name, params) ->
                FunctionWithParams(
                    uniformPrefix = pair.hash.toString(),
                    function = LennardJones,
                    params = params,
                )
            }
        )
    }

    @Transient
    val configurableUniforms: List<UniformParameter<*>> = pairwiseInteraction
        .flatMap { it.uniforms }
}
