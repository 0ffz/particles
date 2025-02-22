package me.dvyy.particles.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.dvyy.particles.dsl.pairwise.*

@Serializable
data class Simulation(
    @SerialName("count")
    val targetCount: Int = 10_000,
    val conversionRate: Int = 100,
    val minGridSize: Double = 5.0,
    val dT: Double = 0.001,
    val maxVelocity: Double = 20.0,
    val maxForce: Double = 100000.0,
    val threeDimensions: Boolean = false,
    val passesPerFrame: Int = 100,
    val size: Size = Size(),
)

@Serializable
data class Size(val width: Int = 1000, val height: Int = 1000, val depth: Int = 1000)

@Serializable
data class ParticlesConfig(
    val simulation: Simulation = Simulation(),
    val application: ApplicationConfiguration = ApplicationConfiguration(),
    @SerialName("particles")
    val nameToParticle: Map<String, Particle> = mapOf(),
    val interactions: Map<String, Map<String, Map<String, Parameter<Float>>>> = mapOf(),
) {
    @Transient
    val particles = nameToParticle.values.toList()

    @Transient
    val functions: List<PairwiseFunction> = listOf(LennardJones)

    @Transient
    private var highestId = 0u

    @Transient
    val particleIds: Map<String, ParticleId> = nameToParticle.mapValues { ParticleId(highestId++) }

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
