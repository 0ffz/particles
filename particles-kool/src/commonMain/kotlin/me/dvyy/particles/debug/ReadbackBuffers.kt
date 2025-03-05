package me.dvyy.particles.debug

import de.fabmax.kool.util.launchOnMainThread
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.ConfigRepository

object ReadbackBuffers {
    fun readValues(buffers: ParticleBuffers, configRepo: ConfigRepository) = launchOnMainThread {
            buffers.particleTypesBuffer.readbackBuffer()
            buffers.positionBuffer.readbackBuffer()
            buffers.velocitiesBuffer.readbackBuffer()
            buffers.particleGridCellKeys.readbackBuffer()
            buffers.sortIndices.readbackBuffer()
            buffers.offsetsBuffer.readbackBuffer()
            val count = configRepo.count
            println("Positions: " + (0 until count).map { buffers.positionBuffer.getF4(it) }.toString())
            println("Velocities: " + (0 until count).map { buffers.velocitiesBuffer.getF4(it) }.toString())
            println("Keys: " + (0 until count).map { buffers.particleGridCellKeys.getI1(it) }.toString())
            println("Indices: " + (0 until count).map { buffers.sortIndices.getI1(it) }.toString())
            println("Offsets: " + (0 until count).map { buffers.offsetsBuffer.getI1(it) }.toString())
    }
}
