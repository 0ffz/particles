package me.dvyy.particles.helpers

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer
import de.fabmax.kool.util.Float32Buffer
import kotlin.random.Random

object Buffers {
    fun positions(count: Int, size: Vec3f) = float4(count) {
        Vec4f(
            Random.Default.nextDouble(size.x.toDouble()).toFloat(),
            Random.Default.nextDouble(size.y.toDouble()).toFloat(),
            if (size.z == 0f) 0f else Random.Default.nextDouble(size.z.toDouble()).toFloat(),
            0f
        )
    }

    fun velocities(count: Int, threeDimensional: Boolean, maxVelocity: Double) = float4(count) {
        //TODO actually cap at maxVelocity
        Vec4f(
            Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
            Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
            if (threeDimensional) Random.nextDouble(-maxVelocity, maxVelocity).toFloat() else 0f,
            0f,
        )
    }

    fun integers(count: Int) = StorageBuffer(GpuType.Int1, count)

    inline fun floats(count: Int, set: (Int) -> Float = { 0f }) = StorageBuffer(GpuType.Float1, count).apply {
        uploadData(Float32Buffer(count).apply {
            for (i in 0 until count) {
                this[i] = set(i)
            }
        })
    }

    inline fun float4(count: Int, set: (Int) -> Vec4f = { Vec4f.ONES }): StorageBuffer {
        return StorageBuffer(GpuType.Float4, count).apply {
            uploadData(float32_4(count, set))
        }
    }

    inline fun float32_4(count: Int, set: (Int) -> Vec4f = { Vec4f.ONES }) = Float32Buffer(count * 4).apply {
        for (i in 0 until count) {
            val default = set(i)
            this[4 * i] = default.x
            this[4 * i + 1] = default.y
            this[4 * i + 2] = default.z
            this[4 * i + 3] = default.w
        }
    }
}
