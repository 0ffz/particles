package me.dvyy.particles.compute.data

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.helpers.ResetIntsShader
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.helpers.Buffers

class VelocitiesDataShader(
    val buffers: ParticleBuffers,
) {
    val numBuckets = 64
    val reduce = KslComputeShader("Velocities Data") {
        computeStage(WORK_GROUP_SIZE) {
            val inputs = storage<KslFloat4>("inputs")
            val buckets = storage<KslInt1>("buckets")
            val numBuckets = uniformInt1("numBuckets")
            val maxVelocity = uniformFloat1("maxVelocity")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val velocity = float3Var(inputs[id].xyz)
                val length = float1Var(length(velocity))
                val bucket = (length / maxVelocity * numBuckets.toFloat1()).toInt1()
                //FIXME int1Var is needed since atomicAdd doesn't get called otherwise.
                // Report to kool-engine.
                int1Var(buckets.atomicAdd(bucket, 1.const))
            }
        }
    }

    val buckets = Buffers.integers(numBuckets)

    fun addTo(pass: ComputePass) {
        println("adding velocities data shader")
        reduce.apply {
            storage("inputs", buffers.velocitiesBuffer)
            storage("buckets", buckets)
            uniform1i("numBuckets", numBuckets)
        }
        pass.addTask(ResetIntsShader(buckets), Vec3i(1, 1, 1))
        pass.addTask(reduce, buffers.configRepo.numGroups).onBeforeDispatch {
            buffers.configRepo.whenDirty {
                reduce.uniform1f("maxVelocity").set(buffers.configRepo.config.value.simulation.maxVelocity.toFloat())
            }
        }
    }
}
