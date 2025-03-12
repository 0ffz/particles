import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.config.ConfigRepository

/**
 * Calculates the start indices in the full particles buffer for each grid cell (where cells are keys, and offsets
 */
class OffsetsShader(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
) {
    val shader = KslComputeShader("Offset Compute") {
        computeStage(WORK_GROUP_SIZE) {
            val numValues = uniformInt1("numValues")
            val keysBuffer = storage<KslInt1>("keys")
            val offsetsBuffer = storage<KslInt1>("offsets")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())

                // Early return if beyond numValues
                `if`(id lt numValues) {
                    val notPresent = int1Var(numValues);
                    val key = int1Var(keysBuffer[id])
                    val keyPrev = int1Var(notPresent)
                    `if`(id ne 0.const) {
                        keyPrev set keysBuffer[id - 1.const]
                    }
                    `if`(key ne keyPrev) { offsetsBuffer[key] = id }
                }
            }
        }
    }

    var numValues by shader.uniform1i("numValues")
    var keys by shader.storage("keys")
    var offsets by shader.storage("offsets")

    fun addTo(pass: ComputePass) {
        numValues = configRepo.count
        keys = buffers.particleGridCellKeys
        offsets = buffers.offsetsBuffer
        pass.addTask(shader, numGroups = configRepo.numGroups)
    }
}
