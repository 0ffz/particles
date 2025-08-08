package me.dvyy.particles

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.util.Int32Buffer
import kotlinx.coroutines.test.runTest
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.execManyShaders
import me.dvyy.particles.compute.partitioning.GPUSort
import me.dvyy.particles.compute.partitioning.ReorderBuffersShader
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.dsl.Size
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.helpers.initInt
import me.dvyy.particles.helpers.kool.KoolTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ShaderTest : KoolTest() {
    @Test
    fun `gpu sort should correctly sort unsorted keys`() = runTest {
        val count = 1280
        val config = ConfigRepository(AppSettings()).apply {
            updateConfig(
                ParticlesConfig(
                    simulation = Simulation(
                        count = count,
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
            sort.addSortingShader(count, buffers, it)
        }, read = {
            val result = Int32Buffer(buffers.particleGridCellKeys.size)
            buffers.particleGridCellKeys.downloadData(result)
            result
        }).await()
        assertEquals(
            unsortedBuffer.toArray().apply { sort() }.toList(),
            result.toArray().toList()
        )
    }

    @Test
    fun `reorder should correctly move buffers based on shuffled indices`() = runTest {
        val count = 100000
        val shuffled = (0 until count).shuffled()
        val indicesBuffer = Buffers.integers(count).initInt {
//            if (it % 2 == 0) it + 1 else it - 1
            shuffled[it]
//            count - it - 1
        }
        val data = Buffers.integers(count).initInt { it }
        val shader = ReorderBuffersShader(
            buffersToSort = listOf(data),
        )
        val result = Int32Buffer(count)
        execManyShaders(scene, setup = {
            shader.addTo(it, indicesBuffer, count, Vec3i(count / WORK_GROUP_SIZE + 1, 1, 1))
        }) {
            data.downloadData(result)
        }.join()

        assertContentEquals(
            shuffled.toList(),
            result.toArray().toList(),
        )
    }
}
