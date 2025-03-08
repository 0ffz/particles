package me.dvyy.particles.forces

import me.dvyy.particles.forces.individual.ConstantForce
import me.dvyy.particles.forces.pairwise.LennardJones
import me.dvyy.particles.launchParticles

fun main() = launchParticles(
    forces = listOf(
        LennardJones,
        ConstantForce
    )
)
