package me.dvyy.particles.forces.pairwise

import de.fabmax.kool.modules.ksl.lang.minus
import de.fabmax.kool.modules.ksl.lang.times
import de.fabmax.kool.modules.ksl.lang.unaryMinus
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

/**
 * Derivative of the [Morse potential](https://en.wikipedia.org/wiki/Morse_potential#Potential_energy_function).
 */
object Morse : PairwiseForce("morse") {
    /** Well depth */
    val De = param<Float>("De")
    /** Well 'width' */
    val a = param<Float>("a")
    /** Equilibrium bond distance */
    val re = param<Float>("re")

    override fun KslPairwiseFunction.createFunction() {
        val De = De.asShaderParam()
        val a = a.asShaderParam()
        val re = re.asShaderParam()

        body {
            val exponential = float1Var(exp(-a * (distance - re)))
            (-2f).const * a * De * exponential * (1f.const - exponential)
        }
    }
}
