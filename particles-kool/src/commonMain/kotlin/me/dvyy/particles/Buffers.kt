package me.dvyy.particles

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer1d
import kotlin.random.Random

object Buffers {
    fun positions(count: Int, width: Int, height: Int) =  StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] =
                Vec4f(Random.Default.nextInt(width).toFloat(), -Random.Default.nextInt(height).toFloat(), -10f, 0f)
        }
    }

    fun integers(count: Int) = StorageBuffer1d(count, GpuType.INT1)/*.apply {
        for (i in 0 until count) {
            this[i] = 0
        }
    }*/
}
