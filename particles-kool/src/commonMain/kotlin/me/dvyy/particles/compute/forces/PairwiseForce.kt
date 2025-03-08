package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

/**
 * A force between two particles, given only distance as a parameter
 */
abstract class PairwiseForce(name: String): Force(name) {
    abstract fun KslPairwiseFunction.createFunction()

    override fun createFunction(stage: KslComputeStage) {
        KslPairwiseFunction(stage, name).apply { createFunction() }
    }
}

