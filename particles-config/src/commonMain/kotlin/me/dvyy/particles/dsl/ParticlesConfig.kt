package me.dvyy.particles.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal typealias Parameters = Map<String, Float>
internal typealias InteractionName = String
internal typealias PairName = String

@Serializable
data class ParticlesConfig(
    val simulation: Simulation = Simulation(),
    val application: ApplicationConfiguration = ApplicationConfiguration(),
    @SerialName("particles")
    val nameToParticle: Map<String, Particle> = mapOf(),
    val interactions: Map<InteractionName, Map<PairName, Parameters>> = mapOf(),
) {
    @Transient
    val particles = nameToParticle.values.toList()

    val particleTypeCount = particles.size

    @Transient
    private var highestId = 0

    @Transient
    val particleIds: Map<String, ParticleId> = nameToParticle.mapValues { ParticleId(highestId++) }

    fun particle(name: String): ParticleId = particleIds[name] ?: error("Particle with name $name not found")

    fun particleName(id: ParticleId) = particleIds.entries.firstOrNull { it.value == id }?.key ?: error("Particle with id $id not found")
//    @Transient
//    val pairwiseInteractions: Map<InteractionName, Map<ParticlePair, Parameters>> = interactions.mapNotNull { (name, interactions) ->
//        val pair = ParticlePair.fromString(name, particleIds) ?: return@mapNotNull null
//        pair to interactions
//    }.toMap()
//
//    @Transient
//    val individualInteractions: Map<ParticleId, Map<PairName, Parameters>> = interactions.mapNotNull { (name, interactions) ->
//        val individual = particleIds[name] ?: return@mapNotNull null
//        individual to interactions
//    }.toMap()
}
