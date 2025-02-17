package me.dvyy.particles

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer1d
import kotlin.random.Random

object Buffers {
    fun positions(count: Int, width: Int, height: Int, depth: Int) = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] =
                Vec4f(
                    Random.Default.nextInt(width).toFloat(),
                    Random.Default.nextInt(height).toFloat(),
                    if (depth == 0) 0f else Random.Default.nextInt(depth).toFloat(),
                    0f
                )
        }
    }

    fun velocities(count: Int, threeDimensional: Boolean, maxVelocity: Double) = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            //TODO actually cap at maxVelocity
            this[i] = Vec4f(
                Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
                Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
                if(threeDimensional) Random.nextDouble(-maxVelocity, maxVelocity).toFloat() else 0f,
                0f,
            )
        }
    }

    fun integers(count: Int) = StorageBuffer1d(count, GpuType.INT1)
    fun floats(count: Int) = StorageBuffer1d(count, GpuType.FLOAT1)

    fun float4(count: Int) = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] = Vec4f(1f, 1f, 1f, 1f)
        }
    }
}
