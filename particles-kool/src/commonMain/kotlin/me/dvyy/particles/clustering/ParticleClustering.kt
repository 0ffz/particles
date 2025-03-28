package me.dvyy.particles.clustering

import de.fabmax.kool.Assets
import de.fabmax.kool.MimeType
import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.Int32Buffer
import de.fabmax.kool.util.launchOnMainThread
import de.fabmax.kool.util.toBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.toSvg
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.xlim

/**
 * Calculates the start indices in the full particles buffer for each grid cell (where cells are keys, and offsets
 */
class ParticleClustering(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
    val viewModel: ParticlesViewModel,
) {
    fun calculateClusters() = launchOnMainThread {
        val positions = Float32Buffer(configRepo.count * 4)
        buffers.positionBuffer.downloadData(positions)
        val data = Array(configRepo.count) {
            doubleArrayOf(
                positions[it * 4].toDouble(),
                positions[it * 4 + 1].toDouble(),
                positions[it * 4 + 2].toDouble()
            )
        }
        val clusters = withContext(Dispatchers.Default) {
            cluster(data, radius = 15.0, minPts = 10)
        }
        buffers.clustersBuffer.uploadData(
            Int32Buffer(configRepo.count).apply {
                clusters.clusters.forEachIndexed { i, cluster ->
                    set(i, cluster)
                }
            },
        )

        val texture = withContext(Dispatchers.Default) {
            val svg = createPlot(clusters).toSvg()
            Assets.loadImageFromBuffer(svg.encodeToByteArray().toBuffer(), MimeType.IMAGE_SVG)
        }
        viewModel.plotTexture.upload(texture)
    }

    fun createPlot(clusters: ClusterInfo): Plot {
        val data = mapOf<String, Any>(
            "size" to clusters.sizes.sorted().dropLast(1) // drop the largest value (usually the outlier cluster)
        )

        return letsPlot(data) {
            labs(
                caption = "Cluster size distribution",
                x = "size"
            )
            xlim(0 to null)
        } + geomHistogram(
            color = "dark-blue",
            fill = "blue",
            alpha = .3,
            size = 1.0
        ) {
            x = "size"
        }
    }
}
