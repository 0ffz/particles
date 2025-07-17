package me.dvyy.particles.helpers

import de.fabmax.kool.modules.ksl.lang.times
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

internal object TestForce : PairwiseForce("test_force") {
    val scalar = param<Float>("scalar")
    override fun KslPairwiseFunction.createFunction() {
        body {
            scalar.asShaderParam() * sqrt(distance)
        }
    }
}
