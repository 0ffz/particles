package me.dvyy.particles.helpers

import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

internal object TestForce : PairwiseForce("test_force") {
    override fun KslPairwiseFunction.createFunction() {
        body {
            sqrt(distance)
        }
    }
}
