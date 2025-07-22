package me.dvyy.particles

import de.fabmax.kool.util.Struct
import de.fabmax.kool.util.StructBuffer
import kotlinx.coroutines.runBlocking
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.ParticleStruct
import me.dvyy.particles.compute.partitioning.GPUSort
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.dsl.Size
import me.dvyy.particles.helpers.kool.KoolTest
import me.dvyy.particles.ui.nodes.execManyShaders
import org.junit.jupiter.api.Test

class ShaderTest : KoolTest() {
    //    lateinit var testCtx: KoolContext
//    lateinit var scene: Scene
//    val startup = Job()
//    val testContext = CoroutineScope(Dispatchers.Default)

    @Test
    fun `gpu sort should correctl sort unsorted keys`(): Unit = runBlocking {
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
//        val unsortedBuffer = Int32Buffer(buffers.particleGridCellKeys.size).apply {
//            repeat(buffers.particleGridCellKeys.size) {
//                put(Random.nextInt(0, 1000))
//            }
//        }
//        buffers.particleGridCellKeys.uploadData(unsortedBuffer)
        val result = execManyShaders(scene, setup = {
            sort.addResetShader(it)
//            sort.addSortingShader(1000, it)
        }, read = {
             val result = StructBuffer(buffers.particleBuffer.size, ParticleStruct())
            buffers.particleBuffer.downloadData(result)
            result
        }).await()

//        launchOnMainThread {
//            val result = StructBuffer(2000, ParticleStruct())
//            buffers.particleBuffer.downloadData(result)
            println(result.map { position.get() })
            println(result.map { velocity.get() })
            println(result.map { force.get() })
            println(result.map { gridCellId.get() })

//        }.join()
//        assertContentEquals(
//            unsortedBuffer.toArray().apply { sort() },
//            result.toArray(),
//            "Resulting buffer was not sorted fully"
//        )
    }
}

fun <T: Struct, R> StructBuffer<T>.map(transform: T.() -> R): List<R> {
    forEach {  }
    return buildList {
        repeat(this@map.size) {
            this@buildList.add(transform(this@map[it]))
        }
    }
}
