package me.dvyy.particles.dsl

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.dvyy.particles.dsl.pairwise.ParticlePair

typealias InteractionConfig = Map<String, Parameter<YamlNode>>
typealias Interactions = Map<String, InteractionConfig>

@Serializable
data class ParticlesConfig(
    val simulation: Simulation = Simulation(),
    val application: ApplicationConfiguration = ApplicationConfiguration(),
    @SerialName("particles")
    val nameToParticle: Map<String, Particle> = mapOf(),
    val interactions: Map<String, Interactions> = mapOf(),
) {
    @Transient
    val particles = nameToParticle.values.toList()

    @Transient
    private var highestId = 0u

    @Transient
    val particleIds: Map<String, ParticleId> = nameToParticle.mapValues { ParticleId(highestId++) }

    @Transient
    val pairwiseInteractions: Map<ParticlePair, Interactions> = interactions.mapNotNull { (name, interactions) ->
        val pair = ParticlePair.fromString(name, particleIds) ?: return@mapNotNull null
        pair to interactions
    }.toMap()

    @Transient
    val individualInteractions = interactions.mapNotNull { (name, interactions) ->
        val individual = particleIds[name] ?: return@mapNotNull null
        individual to interactions
    }.toMap()
//    @Transient
//    val pairwiseInteractions: List<PairwiseInteractions> = interactions.map { (name, interactions) ->
//        val (first, second) = name.split("-")
//        val pair = ParticlePair(particleIds[first]!!, particleIds[second]!!)
//        PairwiseInteractions(
//            pair,
//            interactions.map { (name, params) ->
//                FunctionWithParams(
//                    uniformPrefix = pair.hash.toString(),
//                    function = LennardJones,
//                    params = params,
//                )
//            }
//        )
//    }
//
//    @Transient
//    val configurableUniforms: List<UniformParameter<*>> = pairwiseInteractions
//        .flatMap { it.uniforms }
}
