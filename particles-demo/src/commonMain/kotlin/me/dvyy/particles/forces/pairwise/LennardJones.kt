package me.dvyy.particles.forces.pairwise

import de.fabmax.kool.modules.ksl.lang.div
import de.fabmax.kool.modules.ksl.lang.minus
import de.fabmax.kool.modules.ksl.lang.times
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction
import me.dvyy.particles.compute.forces.PairwiseForce

/**
 * Derivative of the [Lennard Jones potential](https://en.wikipedia.org/wiki/Lennard-Jones_potential)
 */
object LennardJones : PairwiseForce("lennardJones") {
    val sigma = param<Float>("sigma")
    val epsilon = param<Float>("epsilon")

    override fun KslPairwiseFunction.createFunction() {
        val sigma = sigma.asShaderParam()
        val epsilon = epsilon.asShaderParam()

        body {
            val invR = float1Var(sigma / distance)
            val invR6 = float1Var(invR * invR * invR * invR * invR * invR)
            val invR12 = float1Var(invR6 * invR6)
            min(24f.const * epsilon * (2f.const * invR12 - invR6) / distance, maxForce)
        }
    }
}
