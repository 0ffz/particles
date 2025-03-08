package me.dvyy.particles.compute

import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.compute.forces.IndividualForce
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.pairwise.ParticlePair

class ForcesDefinition(
    val forces: List<Force>,
    val config: ParticlesConfig,
) {
    val pairwiseForces = forces.filterIsInstance<PairwiseForce>()
    val individualForces = forces.filterIsInstance<IndividualForce>()

    val pairwiseInteractions: Map<ParticlePair, List<ConfiguredFunction>> =
        config.pairwiseInteractions.map { (pair, interactions) ->
            val forces = interactions.map { (name, config) ->
                val function = pairwiseForces.find { it.name == name } ?: error("Unknown pairwise interaction: $name")
                ConfiguredFunction(function, config)
            }
            pair to forces
        }.toMap()

    val individualInteractions = config.individualInteractions.map { (particle, interactions) ->
        //TODO repeated code
        val forces = interactions.map { (name, config) ->
            ConfiguredFunction(individualForces.find { it.name == name }!!, config)
        }
        particle to forces
    }.toMap()
}
