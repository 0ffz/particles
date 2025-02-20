package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Releasable
import de.fabmax.kool.util.releaseWith
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.Particle
import me.dvyy.particles.helpers.Buffers

class ParticleBuffers(
    val configRepo: ConfigRepository,
): Releasable {
    val count = configRepo.count
    val particleTypes: List<Particle> = configRepo.config.value.particles
    val positionBuffer = Buffers.positions(count, configRepo.boxSize)
    val velocitiesBuffer = Buffers.velocities(count, configRepo.config.value.simulation.threeDimensions, 20.0)

    val forcesBuffer = Buffers.float4(count, default = Vec4f.ZERO)
    val particleGridCellKeys = Buffers.integers(count)
    val sortIndices = Buffers.integers(count)
    val offsetsBuffer = Buffers.integers(count)
    val colorsBuffer = Buffers.float4(count)

    val particleTypesBuffer = Buffers.integers(count)
    val particleColors = Buffers.float4(particleTypes.size).apply {
        for (i in particleTypes.indices) this[i] = Color(particleTypes[i].color).toVec4f()
    }

    val particleRadii = Buffers.floats(particleTypes.size).apply {
        for (i in particleTypes.indices) this[i] = particleTypes[i].radius.toFloat()
    }

    init {
        initializeParticlesBuffer()
    }

    fun initializeParticlesBuffer() {
        val distTotal = particleTypes.sumOf { it.distribution }
        val counts = particleTypes.map { ((it.distribution / distTotal) * count).toInt() }

        if (particleTypes.isEmpty()) return

        // Add particles based on distribution, ensuring we always get exactly `count` particles
        var type = 0
        var offset = 0
        repeat(count) {
            particleTypesBuffer[it] = type
            if (it - offset >= counts[type] && it != counts.lastIndex) {
                type++
                offset = it
            }
        }
    }

    override var isReleased: Boolean = false

    override fun release() {
        isReleased = true
        positionBuffer.release()
        velocitiesBuffer.release()
        forcesBuffer.release()
        particleGridCellKeys.release()
        sortIndices.release()
        offsetsBuffer.release()
        colorsBuffer.release()
        particleTypesBuffer.release()
        particleColors.release()
        particleRadii.release()
    }
}
