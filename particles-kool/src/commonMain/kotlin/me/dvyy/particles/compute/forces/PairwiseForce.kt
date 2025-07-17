package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction
import me.dvyy.particles.compute.helpers.KslInt
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE

/**
 * A force between two particles, given only distance as a parameter
 */
abstract class PairwiseForce(name: String) : Force(name) {
    abstract fun KslPairwiseFunction.createFunction()

    override fun createFunction(stage: KslComputeStage): KslPairwiseFunction {
        return KslPairwiseFunction(stage, name).apply { createFunction() }
    }

    context(stage: KslComputeStage)
    val kslReference get(): KslFunctionFloat1 = stage.functions[name] as KslFunctionFloat1

    fun createForceComputeShader() = KslComputeShader("force-one-shot") {
        computeStage(WORK_GROUP_SIZE) {
            val localNeighbors = uniformFloat1("localNeighbors")
            val lastIndex = uniformInt1("lastIndex")
            val distances = storage<KslFloat1>("distances")
            val output = storage<KslFloat1>("outputBuffer")

            val function = createFunction(this)
            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                `if`(id le lastIndex) {
                    output[id] = function.function.invoke(distances[id], localNeighbors)
                }
            }
        }
    }

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

