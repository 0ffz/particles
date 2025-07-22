package me.dvyy.particles.helpers

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.pipeline.GpuBuffer
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer
import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.Int32Buffer
import de.fabmax.kool.util.Struct
import de.fabmax.kool.util.StructBuffer
import kotlin.random.Random

object Buffers {
    // TODO 3d float buffers aren't supported, consider swapping to structs
//    fun positions(count: Int, size: Vec3f) = StorageBuffer(GpuType.Float4, count).initFloat4 {
//        randomPosition(size)
//    }

    fun randomPosition(size: Vec3f) = Vec3f(
        Random.Default.nextDouble(size.x.toDouble()).toFloat(),
        Random.Default.nextDouble(size.y.toDouble()).toFloat(),
        if (size.z == 0f) 0f else Random.Default.nextDouble(size.z.toDouble()).toFloat(),
    )

    fun randomVelocity(threeDimensional: Boolean, maxVelocity: Double) = Vec3f(
        Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
        Random.nextDouble(-maxVelocity, maxVelocity).toFloat(),
        if (threeDimensional) Random.nextDouble(-maxVelocity, maxVelocity).toFloat() else 0f,
    )

    fun integers(count: Int) = StorageBuffer(GpuType.Int1, count)

    inline fun floats(count: Int, set: (Int) -> Float = { 0f }) = StorageBuffer(GpuType.Float1, count).apply {
        uploadData(Float32Buffer(count).apply {
            for (i in 0 until count) {
                this[i] = set(i)
            }
        })
    }
}

fun <T : Struct> StructBuffer<T>.init(init: T.(index: Int) -> Unit): StructBuffer<T> {
    repeat(size) {
        set(it) {
            init(it)
        }
    }
    return this
}

inline fun GpuBuffer.initFloat3(set: (Int) -> Vec3f) = apply {
    uploadData(
        Float32Buffer(size * 3).apply {
            repeat(size) { i ->
                val value = set(i)
                put(value.x)
                put(value.y)
                put(value.z)
            }
        }
    )
}

inline fun GpuBuffer.initFloat4(set: (Int) -> Vec4f) = apply {
    uploadData(
        Float32Buffer(size * 4).apply {
            repeat(size) { i ->
                val value = set(i)
                put(value.x)
                put(value.y)
                put(value.z)
                put(value.w)
            }
        }
    )
}

inline fun GpuBuffer.initInt(set: (Int) -> Int) = apply {
    uploadData(Int32Buffer(size).apply { repeat(size) { i -> put(set(i)) } })
}

inline fun GpuBuffer.initFloat(set: (Int) -> Float) = apply {
    uploadData(Float32Buffer(size).apply { repeat(size) { i -> put(set(i)) } })
}
