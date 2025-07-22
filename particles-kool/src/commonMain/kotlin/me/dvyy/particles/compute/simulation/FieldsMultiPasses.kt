package me.dvyy.particles.compute.simulation

import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.forces.ForcesDefinition
import me.dvyy.particles.config.ConfigRepository

class FieldsMultiPasses(
    val buffers: ParticleBuffers,
    val configRepo: ConfigRepository,
    val halfStep: VerletHalfStepShader,
    val fields: FieldsShader,
    val forcesDefinition: ForcesDefinition
) {
    private fun initBuffers() {
        fields.gridSize = configRepo.gridSize
        fields.gridCells = configRepo.gridCells
        fields.count = configRepo.count
        fields.particles = buffers.particleBuffer
        fields.cellOffsets = buffers.offsetsBuffer
//        fields.particle2CellKey = buffers.particleGridCellKeys
        fields.boxMax = configRepo.boxSize

        halfStep.particles = buffers.particleBuffer
        halfStep.boxMax = configRepo.boxSize
    }

    fun addTo(computePass: ComputePass): List<Pair<ComputePass.Task, ComputePass.Task>> {
        val config = configRepo.config.value

        initBuffers()
        val passes = buildList {
            repeat(config.simulation.passesPerFrame) { passIndex ->
                val halfStep = computePass.addTask(halfStep.shader, numGroups = configRepo.numGroups).apply {
                    onBeforeDispatch {
                        configRepo.whenDirty {
                            halfStep.dT = simulation.dT.toFloat()
                            val count = simulation.count
                            setNumGroupsByInvocations(count)
                        }
                    }
                }
                val fullStep = computePass.addTask(fields.shader, numGroups = configRepo.numGroups).apply {
                    onBeforeDispatch {
                        configRepo.whenDirty {
                            fields.dT = simulation.dT.toFloat()
                            fields.params.set {
                                maxVelocity.set(simulation.maxVelocity.toFloat())
                                maxForce.set(simulation.maxForce.toFloat())
                            }
                            val count = simulation.count
                            fields.count = count
                            setNumGroupsByInvocations(count)
                        }
                        //TODO whenDirty
                        with(shader) {
                            forcesDefinition.forces.forEach {
                                it.uploadParameters()
                            }
                        }
                    }
                }
                add(halfStep to fullStep)
            }
        }
        return passes
    }
}
