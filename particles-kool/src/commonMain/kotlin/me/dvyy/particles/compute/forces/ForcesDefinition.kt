package me.dvyy.particles.compute.forces

import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.pairwise.ParticleSet

class ForcesDefinition(
    val forceTypes: List<Force>,
    private val config: ParticlesConfig,
) {
    val particleTypeCount = config.particles.size
    val forces: List<ForceWithParameters<*>> = config.interactions.map { (name, params) ->
        val force = forceTypes.find { it.name == name } ?: error("Unknown force: $name")
        ForceWithParameters(force, particleTypeCount).apply {
            params.forEach { (key, values) ->
                val set = with(config) { ParticleSet.fromString(key) }
                put(set, force.parseParameters(values))
            }
        }
    }

    val pairwiseForces: List<ForceWithParameters<PairwiseForce>> = forces
        .filter { it.force is PairwiseForce }
            as List<ForceWithParameters<PairwiseForce>>

    val individualForces: List<ForceWithParameters<IndividualForce>> = forces
        .filter { it.force is IndividualForce }
            as List<ForceWithParameters<IndividualForce>>

}
