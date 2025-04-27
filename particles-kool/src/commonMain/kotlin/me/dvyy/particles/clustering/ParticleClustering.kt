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
import me.dvyy.particles.config.ClusterOptions
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
    fun calculateClusters(options: ClusterOptions) = launchOnMainThread {
        val positions = Float32Buffer(configRepo.count * 4)
        val types = Int32Buffer(configRepo.count)
        buffers.positionBuffer.downloadData(positions)
        buffers.particleTypesBuffer.downloadData(types)
        val allowedTypes = intArrayOf(configRepo.config.value.particleIds["monomer"]!!.id.toInt())

        val data = mutableListOf<DoubleArray>()
        repeat(configRepo.count) {
            if (types[it] in allowedTypes) data += doubleArrayOf(
                positions[it * 4].toDouble(),
                positions[it * 4 + 1].toDouble(),
                positions[it * 4 + 2].toDouble()
            )
        }
        // TODO because this operation takes a while, by the time it's finished
        //  the uploaded data is out of order due to us sorting particles.
        //  This leads to some visual issues, but the data is otherwise correct.
        val clusters = withContext(Dispatchers.Default) {
            cluster(data, radius = options.radius, minPts = options.minPoints)
        }
        buffers.clustersBuffer.uploadData(
            Int32Buffer(configRepo.count).apply {
                clusters.clusters.forEachIndexed { i, cluster ->
                    set(i, cluster)
                }
            },
        )
        buffers.clusterInfo = clusters

        if(!options.drawGraph) return@launchOnMainThread

        val texture = withContext(Dispatchers.Default) {
            val svg = createPlot(clusters).toSvg()
            Assets.loadImageFromBuffer(svg.encodeToByteArray().toBuffer(), MimeType.IMAGE_SVG)
        }
        viewModel.plotTexture.upload(texture)
    }

    fun createPlot(clusters: ClusterInfo): Plot {
        val data = mapOf<String, Any>(
            "size" to clusters.sizes.sorted().dropLast(10) // drop the largest value (usually the outlier cluster)
        )

        return letsPlot(data) {
            labs(
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
