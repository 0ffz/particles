package me.dvyy.particles.forces

import me.dvyy.particles.forces.individual.ConstantForce
import me.dvyy.particles.forces.pairwise.LennardJones
import me.dvyy.particles.forces.pairwise.Morse
import me.dvyy.particles.forces.pairwise.TersoffSimple
import me.dvyy.particles.launchParticles

fun main(args: Array<String>) {
    launchParticles(
        forces = listOf(
            LennardJones,
            Morse,
            TersoffSimple,
            ConstantForce
        ),
        args = args
    )
}
