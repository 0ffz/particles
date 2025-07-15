package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

/**
 * A force between two particles, given only distance as a parameter
 */
abstract class PairwiseForce(name: String) : Force(name) {
    abstract fun KslPairwiseFunction.createFunction()

    override fun createFunction(stage: KslComputeStage): KslPairwiseFunction {
        return KslPairwiseFunction(stage, name).apply { createFunction() }
    }

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
}

