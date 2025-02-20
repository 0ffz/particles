package me.dvyy.particles.helpers

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer1d
import kotlin.random.Random

object Buffers {
    fun positions(count: Int, size: Vec3f) = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] =
                Vec4f(
                    Random.Default.nextDouble(size.x.toDouble()).toFloat(),
                    Random.Default.nextDouble(size.y.toDouble()).toFloat(),
                    if (size.z == 0f) 0f else Random.Default.nextDouble(size.z.toDouble()).toFloat(),
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

    fun float4(count: Int, default: Vec4f = Vec4f.ONES) = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] = default
        }
    }
}
