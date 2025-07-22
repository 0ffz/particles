package me.dvyy.particles

import de.fabmax.kool.util.Int32Buffer
import kotlinx.coroutines.test.runTest
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.partitioning.GPUSort
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.dsl.Size
import me.dvyy.particles.helpers.kool.KoolTest
import me.dvyy.particles.ui.nodes.execManyShaders
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContentEquals

class ShaderTest : KoolTest() {
    //    lateinit var testCtx: KoolContext
//    lateinit var scene: Scene
//    val startup = Job()
//    val testContext = CoroutineScope(Dispatchers.Default)

    @Test
    fun `gpu sort should correctl sort unsorted keys`() = runTest {
        val config = ConfigRepository(AppSettings()).apply {
            updateConfig(
                ParticlesConfig(
                    simulation = Simulation(
                        count = 1000,
                        size = Size(1000, 1000),
                        minGridSize = 5.0
                    )
                )
            )
        }
        val buffers = ParticleBuffers(config)
        val sort = GPUSort(config, buffers)
        val unsortedBuffer = Int32Buffer(buffers.particleGridCellKeys.size).apply {
            repeat(buffers.particleGridCellKeys.size) {
                put(Random.nextInt(0, 1000))
            }
        }
        buffers.particleGridCellKeys.uploadData(unsortedBuffer)
        val result = execManyShaders(scene, setup = {
            sort.addSortingShader(1000, buffers, it)
        }, read = {
            val result = Int32Buffer(buffers.particleGridCellKeys.size)
            buffers.particleGridCellKeys.downloadData(result)
            result
        }).await()
        assertContentEquals(
            unsortedBuffer.toArray().apply { sort() },
            result.toArray(),
            "Resulting buffer was not sorted fully"
        )
    }
}
