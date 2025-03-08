package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import me.dvyy.particles.compute.forces.builders.KslIndividualForceFunction

abstract class IndividualForce(name: String) : Force(name) {
    abstract fun KslIndividualForceFunction.createFunction()

    override fun createFunction(stage: KslComputeStage) {
        KslIndividualForceFunction(stage, name).createFunction()
    }
}
