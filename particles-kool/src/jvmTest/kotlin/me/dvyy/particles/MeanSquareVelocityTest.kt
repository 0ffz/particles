package me.dvyy.particles

import de.fabmax.kool.util.Float32Buffer
import kotlinx.coroutines.test.runTest
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.data.MeanSquareVelocities
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.dsl.Size
import me.dvyy.particles.helpers.kool.KoolTest
import me.dvyy.particles.ui.nodes.execManyShaders
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MeanSquareVelocityTest : KoolTest() {
    @ParameterizedTest
    @CsvSource("1280") //FIXME 1279 fails because we average with 0s towards the end of the power
    fun `should correctly calculate mean square velocity sum`(count: Int) = runTest {
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

        // 12 = 2^2 + 2^2 + 2^2 is the square velocity for each entry, there are count total entries
        // first value should be the sum of all square velocities
        assert(result.toArray().toList().first() == 12f * count)
        println(result.toArray().toList())
    }
}
