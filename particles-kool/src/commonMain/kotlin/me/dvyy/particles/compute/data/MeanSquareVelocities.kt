package me.dvyy.particles.compute.data

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.helpers.Buffers

class MeanSquareVelocities(
    private val buffers: ParticleBuffers,
) {
    private val meanSquareVelocities = KslComputeShader("MeanSquareVelocities") {
        computeStage(WORK_GROUP_SIZE) {
            val inputs = storage<KslFloat4>("velocities")
            val squareVelocities = storage<KslFloat1>("squareVelocities")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val velocity = float3Var(inputs[id].xyz)
                val length = float1Var(length(velocity))
                squareVelocities[id] = length * length
            }
        }
    }

    private val reduce = KslComputeShader("MeanSquareVelocities_reduce") {
        computeStage(WORK_GROUP_SIZE) {
            val inputs = storage<KslFloat1>("inputs")
            val outputs = storage<KslFloat1>("outputs")
            val total = uniformInt1("total")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                // average two neighbouring values, pass to output
                `if`(id lt total) {
                    outputs[id] = inputs[id * 2.const] + inputs[(id * 2.const) + 1.const]
                }
            }
        }
    }
    private var inputs by reduce.storage("inputs")
    private var outputs by reduce.storage("outputs")
    private var total by reduce.uniform1i("total")

    val inputBuffer = Buffers.floats(buffers.count)
    val outputBuffer = Buffers.floats(buffers.count)

    private val roundedUp = 1 shl (32 - (buffers.count - 1).countLeadingZeroBits())
    private val iterations = roundedUp.countTrailingZeroBits()
    val readBack = if (iterations % 2 == 0) inputBuffer else outputBuffer

    fun addTo(
        pass: ComputePass,
    ) {
        pass.apply {
            addTask(meanSquareVelocities.apply {
                storage("velocities", buffers.velocitiesBuffer)
                storage("squareVelocities", inputBuffer)
            }, numGroups = buffers.configRepo.numGroups)
            repeat(iterations) { iteration ->
                addTask(
                    reduce,
                    numGroups = Vec3i(((roundedUp shr iteration) + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE, 1, 1)
                ).apply {
                    pipeline.swapPipelineData("iteration $iteration")
                    inputs = if (iteration % 2 == 0) inputBuffer else outputBuffer
                    outputs = if (iteration % 2 == 0) outputBuffer else inputBuffer
                    total = buffers.count shr iteration
                    onBeforeDispatch {
                        pipeline.swapPipelineData("iteration $iteration")
                    }
                }
            }
        }
    }
}
