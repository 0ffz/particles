package me.dvyy.particles

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.Platform
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.releaseWith
import kotlinx.coroutines.flow.update
import me.dvyy.particles.clustering.ParticleClustering
import me.dvyy.particles.compute.ConvertParticlesShader
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.data.MeanSquareVelocities
import me.dvyy.particles.compute.data.VelocitiesDataShader
import me.dvyy.particles.compute.partitioning.GPUSort
import me.dvyy.particles.compute.partitioning.OffsetsShader
import me.dvyy.particles.compute.partitioning.ReorderBuffersShader
import me.dvyy.particles.compute.partitioning.ResetBuffers
import me.dvyy.particles.compute.simulation.FieldsMultiPasses
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.launch
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.ParticlesMesh
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class ParticlesScene(
    val buffers: ParticleBuffers,
    val configRepo: ConfigRepository,
    val clustering: ParticleClustering,
    val gpuSort: GPUSort,
    val resetBuffers: ResetBuffers,
    val cameraManager: CameraManager,
    val particlesMesh: ParticlesMesh,
    val offsetsShader: OffsetsShader,
    val velocitiesDataShader: VelocitiesDataShader,
    val meanSquareDataShader: MeanSquareVelocities,
//    val reorderBuffersShader: ReorderBuffersShader,
    val convertShader: ConvertParticlesShader,
    val fieldsShader: FieldsMultiPasses,
    val settings: AppSettings,
    val viewModel: ParticlesViewModel,
) {
    val config = configRepo.config.value

    val scene = scene {
        buffers.releaseWith(this)

        // === COMPUTE ===
        val computePass = ComputePass("Particles Compute")
        //TODO placing this lower seems to set velocity to zero at the start. Is any kind of velocity read at certain times causing it to zero out?
        computePass.addTask(particlesMesh.colorShader, configRepo.numGroups) // Recolor particles
        resetBuffers.addResetShader(computePass) // Reset keys and indices based on grid cell particle is in
        gpuSort.addSortingShader(configRepo.count, buffers = buffers, computePass = computePass) // Sort by grid cells

        // Web has a limit of 8 storage buffers per shader stage, accommodate this by running multiple reorder shaders
        val buffersToReorder = listOf(
            buffers.positionBuffer,
            buffers.velocitiesBuffer,
            buffers.forcesBuffer,
            buffers.particleTypesBuffer,
            buffers.clustersBuffer,
            buffers.colorsBuffer,
            buffers.localNeighboursBuffer,
        )
        val chunked = when (KoolSystem.platform) {
            Platform.Javascript -> buffersToReorder.chunked(3)
            else -> listOf(buffersToReorder)
        }
        chunked.forEach {
            ReorderBuffersShader(it).addTo(
                stage = computePass,
                indices = buffers.sortIndices,
                numValues = configRepo.count,
                numGroups = configRepo.numGroups
            )
        }

        offsetsShader.addTo(computePass) // Calculate offsets (start index in particles array for each grid cell)
        val fieldsPasses = fieldsShader.addTo(computePass) // Run force computations based on particle interactions
        convertShader.addTo(computePass) // Convert particles to different types as needed

        // == DATA COLLECTION ==
        velocitiesDataShader.addTo(computePass)
        meanSquareDataShader.addTo(computePass)

        addComputePass(computePass)

        // === RENDERING ===
        cameraManager.manageCameraFor(this)
        addNode(particlesMesh.mesh) // Render particles as instanced mesh

        // === DEBUGGING ===
        var iterations = 0
        var clearNextFrame = false

        /// === Calibrate FPS ===
        var enabledPasses = viewModel.passesPerFrame
        var iter = 0

        launch {
            settings.ui.targetFPS.collect { iter = 0 }
        }
        launch {
            settings.ui.shouldCalibrateFPS.collect {
                if (it) {
                    iter = 0
                    enabledPasses.update { 1 }
                } else {
                    fieldsPasses.forEach { (halfStep, fullStep) ->
                        halfStep.isEnabled = true; fullStep.isEnabled = true
                    }
                    enabledPasses.update { fieldsPasses.size }
                }
            }
        }
        fieldsPasses.forEach { (halfStep, fullStep) -> halfStep.isEnabled = true; fullStep.isEnabled = true }
        onUpdate { (_, ctx) ->
            val targetFps = settings.ui.targetFPS.value
            if (!settings.ui.shouldCalibrateFPS.value) {
                return@onUpdate
            }
            if (iter > config.simulation.passesPerFrame + 20) return@onUpdate
            iter++
            val fps = 1.0f / Time.deltaT
            when {
                fps > targetFps -> enabledPasses.update { (it + 1).coerceAtMost(fieldsPasses.size) }
                fps < targetFps -> enabledPasses.update { (it - 1).coerceAtLeast(1) }
            }
            fieldsPasses.forEachIndexed { i, (halfStep, fullStep) ->
                val enabled = i <= enabledPasses.value
                halfStep.isEnabled = enabled
                fullStep.isEnabled = enabled
            }
        }
        onUpdate {
            iterations++

            // Unmark config repo as dirty in one frame, allows compute shaders to read values
            if (clearNextFrame) {
                configRepo.isDirty = false
                clearNextFrame = false
            }
            if (configRepo.isDirty) clearNextFrame = true
        }
    }
}
