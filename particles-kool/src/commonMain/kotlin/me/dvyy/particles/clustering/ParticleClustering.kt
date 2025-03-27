package me.dvyy.particles.clustering

import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.Int32Buffer
import de.fabmax.kool.util.launchOnMainThread
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.ConfigRepository

/**
 * Calculates the start indices in the full particles buffer for each grid cell (where cells are keys, and offsets
 */
class ParticleClustering(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
) {
    fun read() = launchOnMainThread {
        val positions = Float32Buffer(configRepo.count * 4)
        buffers.positionBuffer.downloadData(positions)
        val data = Array(configRepo.count) {
            doubleArrayOf(
                positions[it * 4].toDouble(),
                positions[it * 4 + 1].toDouble(),
                positions[it * 4 + 2].toDouble()
            )
        }
        val clusters = cluster(data, radius = 10.0, minPts = 5)
        println(clusters.filter { it != Int.MAX_VALUE })
        buffers.clustersBuffer.uploadData(
            Int32Buffer(configRepo.count).apply {
                clusters.forEachIndexed { i, cluster ->
                    set(i, cluster)
                }
            },
        )
    }
}
