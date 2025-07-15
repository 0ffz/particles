package me.dvyy.particles.compute.simulation

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE

class VerletHalfStepShader {
    val shader = KslComputeShader("Fields Half-Step") {
        computeStage(WORK_GROUP_SIZE) {
            val dT = uniformFloat1("dT")
            val positions = storage<KslFloat4>("positions")
            val velocities = storage<KslFloat4>("velocities")
            val forces = storage<KslFloat4>("forces")
            val boxMax = uniformFloat3("boxMax")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val position = float3Var(positions[id].xyz) //p(t)
                val velocity = float3Var(velocities[id].xyz) //v(t)
                val force = float3Var(forces[id].xyz) //a(t)

                // Verlet integration with half-step eliminated p(t + dt) = p(t) + v(t)dT + 1/2 a(t)dT^2
                val nextPosition = float3Var(position + (velocity * dT) + ((force * dT * dT) / 2f.const))

                // Ensure particles are in bounds
                nextPosition set clamp(nextPosition, 1f.const3, boxMax - 1f.const3)
                positions[id] = float4Value(nextPosition, 0f.const)
            }
        }
    }

    var dT by shader.uniform1f("dT")
    var positions by shader.storage("positions")
    var velocities by shader.storage("velocities")
    var forces by shader.storage("forces")
    var boxMax by shader.uniform3f("boxMax")
}
