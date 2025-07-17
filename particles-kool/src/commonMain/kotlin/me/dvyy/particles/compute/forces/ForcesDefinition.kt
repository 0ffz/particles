package me.dvyy.particles.compute.forces

import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.pairwise.ParticlePair

class ForcesDefinition(
    val forceTypes: List<Force>,
    private val config: ParticlesConfig,
) {
    val particleTypeCount = config.particles.size
    val forces: List<ForceWithParameters<*>> = config.interactions.map { (name, params) ->
        val force = forceTypes.find { it.name == name } ?: error("Unknown force: $name")
        ForceWithParameters(force, particleTypeCount).apply {
            params.forEach { (key, values) ->
                val hash = when(force) {
                    is PairwiseForce -> ParticlePair.fromString(key, config.particleIds, particleTypeCount)?.hash ?: error("Unknown particle pair: $key")
                    is IndividualForce -> config.particleIds[key]?.id ?: error("Unknown particle: $key")
                    else -> error("Invalid force type")
                }
                put(hash, force.parseParameters(values))
            }
        }
    }

    val pairwiseForces: List<ForceWithParameters<PairwiseForce>> = forces
        .filter { it.force is PairwiseForce } as List<ForceWithParameters<PairwiseForce>>
//    val individualForces: List<ForceWithParameters<IndividualForce>> =
//        TODO() //forces.filterIsInstance<IndividualForce>()

}
