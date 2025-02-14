package template

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.KslFloat4
import de.fabmax.kool.modules.ksl.lang.plus
import de.fabmax.kool.modules.ksl.lang.toInt1
import de.fabmax.kool.modules.ksl.lang.x

val SimpleMovement = KslComputeShader("SimpleMovement") {
    computeStage(64) {
        val positionsBuffer = storage1d<KslFloat4>("positionsBuffer")
        main {
            val idx = int1Var(inGlobalInvocationId.x.toInt1())
            val pos = float4Var(positionsBuffer[idx])
            positionsBuffer[idx] = pos.plus(float4Value(0.1f, 0f, 0f, 0f))
        }
    }
}
