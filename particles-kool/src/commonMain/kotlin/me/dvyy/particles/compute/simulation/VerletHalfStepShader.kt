package me.dvyy.particles.compute.simulation

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.ParticleStruct
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE

class VerletHalfStepShader {
    val shader = KslComputeShader("Fields Half-Step") {
        computeStage(WORK_GROUP_SIZE) {
            val dT = uniformFloat1("dT")
            val particleStruct = struct { ParticleStruct() }
            val particles = storage("particles", particleStruct)
            val boxMax = uniformFloat3("boxMax")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val particleVar = structVar(particles[id])
                val particle = particleVar.struct
                val position = particle.position.ksl //p(t)
                val velocity = particle.velocity.ksl //v(t)
                val force = particle.force.ksl //a(t)

                // Verlet integration with half-step eliminated p(t + dt) = p(t) + v(t)dT + 1/2 a(t)dT^2
                val nextPosition = float3Var(position + (velocity * dT) + ((force * dT * dT) / 2f.const))

                // Ensure particles are in bounds (periodic boundary)
                nextPosition set clamp(
                    nextPosition,// - (boxMax * floor(nextPosition / boxMax)), // modulo box size
                    0f.const3, boxMax, // clamp in box boundary for the case where boxMax has a dimension with size 0
                )
//                nextPosition set clamp(nextPosition, 0f.const3, boxMax)
                particle.position.ksl set nextPosition
                particles[id] = particleVar
            }
        }
    }

    var dT by shader.uniform1f("dT")
    var particles by shader.storage("particles")
    var boxMax by shader.uniform3f("boxMax")
}
