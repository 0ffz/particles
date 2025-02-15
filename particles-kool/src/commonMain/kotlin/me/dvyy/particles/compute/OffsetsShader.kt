import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.WORK_GROUP_SIZE

/**
 * Calculates
 */
val OffsetsShader = KslComputeShader("Offset Compute") {
    computeStage(WORK_GROUP_SIZE) {
        val numValues = uniformInt1("numValues")
        val keysBuffer = storage1d<KslInt1>("keys")
        val offsetsBuffer = storage1d<KslInt1>("offsets")

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
