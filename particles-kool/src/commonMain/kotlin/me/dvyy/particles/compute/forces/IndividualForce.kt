package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslFunctionFloat3
import me.dvyy.particles.compute.forces.builders.KslIndividualForceFunction

abstract class IndividualForce(name: String) : Force(name) {
    abstract fun KslIndividualForceFunction.createFunction()

    context(stage: KslComputeStage)
    override fun createFunction(): KslIndividualForceFunction {
        return KslIndividualForceFunction(stage, name).apply { createFunction() }
    }

    context(stage: KslComputeStage)
    val kslReference get(): KslFunctionFloat3 = stage.functions[name] as KslFunctionFloat3
}
