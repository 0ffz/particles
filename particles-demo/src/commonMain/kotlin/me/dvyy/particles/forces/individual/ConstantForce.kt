package me.dvyy.particles.forces.individual

import me.dvyy.particles.compute.forces.IndividualForce
import me.dvyy.particles.compute.forces.builders.KslIndividualForceFunction

object ConstantForce : IndividualForce("gravity") {
    val force = param<Float>("force")
    override fun KslIndividualForceFunction.createFunction() {
        val force = force.asShaderParam()
        body {
            float3Value(0f.const, force, 0f.const)
        }
    }
}
