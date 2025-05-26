package me.dvyy.particles

import OffsetsShader
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.releaseWith
import me.dvyy.particles.clustering.ParticleClustering
import me.dvyy.particles.compute.ConvertParticlesShader
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.ParticlesMesh

class ParticlesScene(
    val buffers: ParticleBuffers,
    val configRepo: ConfigRepository,
    val clustering: ParticleClustering,
    val gpuSort: GPUSort,
    val cameraManager: CameraManager,
    val particlesMesh: ParticlesMesh,
    val offsetsShader: OffsetsShader,
    val convertShader: ConvertParticlesShader,
    val fieldsShader: FieldsShader,
    val settings: AppSettings,
) {
    val config = configRepo.config.value

    val scene = scene {
        buffers.releaseWith(this)
        onRelease { println("Released $this") }

        // === COMPUTE ===
        val computePass = ComputePass("Particles Compute")

        gpuSort.addResetShader(computePass) // Reset keys and indices based on grid cell particle is in
        gpuSort.addSortingShader(configRepo.count, buffers = buffers, computePass = computePass) // Sort by grid cells
        offsetsShader.addTo(computePass) // Calculate offsets (start index in particles array for each grid cell)
        fieldsShader.addTo(computePass) // Run force computations based on particle interactions
        convertShader.addTo(computePass) // Convert particles to different types as needed

        addComputePass(computePass)

        // === RENDERING ===
        cameraManager.manageCameraFor(this)
        addNode(particlesMesh.mesh) // Render particles as instanced mesh

        // === DEBUGGING ===
        var iterations = 0
        var clearNextFrame = false
        onUpdate {
            iterations++

            // Unmark config repo as dirty in one frame, allows compute shaders to read values
            if (clearNextFrame)  {
                configRepo.isDirty = false
                clearNextFrame = false
            }
            if (configRepo.isDirty) clearNextFrame = true
        }
    }
}
