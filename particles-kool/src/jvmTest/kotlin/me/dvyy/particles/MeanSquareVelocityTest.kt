package me.dvyy.particles

import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.Int32Buffer
import kotlinx.coroutines.test.runTest
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.data.MeanSquareVelocities
import me.dvyy.particles.compute.data.VelocitiesDataShader
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.dsl.Size
import me.dvyy.particles.helpers.kool.KoolTest
import me.dvyy.particles.ui.nodes.execManyShaders
import org.junit.jupiter.api.Test

class MeanSquareVelocityTest : KoolTest() {
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
        val shader = MeanSquareVelocities(buffers)

        val velocities = Float32Buffer(buffers.count * 4).apply {
            repeat(buffers.count * 4) {
                put(2f)
            }
        }
        buffers.velocitiesBuffer.uploadData(velocities)
        val result = execManyShaders(scene, setup = {
            shader.addTo(it)
        }, read = {
            val result = Float32Buffer(buffers.count)
            shader.outputBuffer.downloadData(result)
            result
        }).await()

        println(result.toArray().toList())
    }
}
