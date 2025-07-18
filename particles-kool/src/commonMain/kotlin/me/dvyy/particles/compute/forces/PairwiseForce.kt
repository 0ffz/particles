package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction
import me.dvyy.particles.compute.helpers.KslInt

/**
 * A force between two particles, given only distance as a parameter
 */
abstract class PairwiseForce(name: String) : Force(name) {
    abstract fun KslPairwiseFunction.createFunction()

    context(stage: KslComputeStage)
    override fun createFunction(): KslPairwiseFunction {
        return KslPairwiseFunction(stage, name).apply { createFunction() }
    }

    context(stage: KslComputeStage)
    val kslReference get(): KslFunctionFloat1 = stage.functions[name] as KslFunctionFloat1

    companion object {
        /** Gets the hash for a pair of particle types (symmetrical), knowing the total particle type count. */
        context(scope: KslScopeBuilder)
        fun pairHash(first: KslInt, second: KslInt, totalParticleTypes: Int): KslInt = with(scope) {
            val min = min(first, second)
            val max = max(first, second)
            return min + max * totalParticleTypes.const
        }
    }
}

