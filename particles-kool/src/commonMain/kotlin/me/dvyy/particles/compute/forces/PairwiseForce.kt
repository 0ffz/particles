package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslFloat4
import de.fabmax.kool.modules.ksl.lang.ksl
import de.fabmax.kool.modules.ksl.lang.struct
import me.dvyy.particles.compute.FieldsShader.SimulationParameters
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
        val params = uniformStruct("params", provider = ::SimulationParameters)
        computeStage(WORK_GROUP_SIZE) {
            val localNeighbors = uniformFloat1("localNeighbors")
            val distances = storage<KslFloat4>("distances")

            val function = createFunction(this)
            main {
//                val id =
                function.function.invoke()
            }
        }
    }
}

