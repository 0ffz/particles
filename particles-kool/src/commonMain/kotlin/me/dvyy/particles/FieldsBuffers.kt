package me.dvyy.particles

import de.fabmax.kool.util.Color
import me.dvyy.particles.dsl.Particle

class FieldsBuffers(
//    scene: Scene,
    particleTypes: List<Particle>,
    val width: Int,
    val height: Int,
    val depth: Int,
    val count: Int,
) {
    val positionBuffers = arrayOf(
        Buffers.positions(count, width, height, depth),
        Buffers.positions(count, width, height, depth)
    )
    val velocitiesBuffers = arrayOf(
        Buffers.velocities(count, depth != 0, 20.0),
        Buffers.velocities(count, depth != 0, 20.0),
    )
    val particleGridCellKeys = Buffers.integers(count)/*.apply {
        for (i in 0 until count) this[i] = count - i - 1//Random.nextInt(count)
    }*/
    val sortIndices = Buffers.integers(count)
    val offsetsBuffer = Buffers.integers(count)
    val colorsBuffer = Buffers.float4(count)

    val particleTypesBuffer = Buffers.integers(count).apply {
        val distTotal = particleTypes.sumOf { it.distribution }
        val counts = particleTypes.map { ((it.distribution / distTotal) * count).toInt() }

        // Add particles based on distribution, ensuring we always get exactly `count` particles
        var type = 0
        var offset = 0
        repeat(count) {
            this[it] = type
            if (it - offset >= counts[type] && it != counts.lastIndex) {
                type++
                offset = it
            }
        }
    }
    val particleColors = Buffers.float4(particleTypes.size).apply {
        for (i in 0 until particleTypes.size) this[i] = Color(particleTypes[i].color).toVec4f()
    }
//
//    init {
//        scene.onRelease {
//            positionBuffers.forEach { it.release() }
//            velocitiesBuffers.forEach { it.release() }
//            particleGridCellKeys.release()
//            sortIndices.release()
//            offsetsBuffer.release()
//            colorsBuffer.release()
//            particleTypesBuffer.release()
//            particleColors.release()
//        }
//    }
}
