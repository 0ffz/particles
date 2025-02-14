package template

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.KslFloat4
import de.fabmax.kool.modules.ksl.lang.b
import de.fabmax.kool.modules.ksl.lang.div
import de.fabmax.kool.modules.ksl.lang.eq
import de.fabmax.kool.modules.ksl.lang.g
import de.fabmax.kool.modules.ksl.lang.minus
import de.fabmax.kool.modules.ksl.lang.plus
import de.fabmax.kool.modules.ksl.lang.r
import de.fabmax.kool.modules.ksl.lang.rem
import de.fabmax.kool.modules.ksl.lang.times
import de.fabmax.kool.modules.ksl.lang.toFloat2
import de.fabmax.kool.modules.ksl.lang.toInt1
import de.fabmax.kool.modules.ksl.lang.toInt2
import de.fabmax.kool.modules.ksl.lang.x
import de.fabmax.kool.modules.ksl.lang.xy
import de.fabmax.kool.modules.ksl.lang.y
import de.fabmax.kool.util.Color

// create a simple compute shader
val computeShader = KslComputeShader("Compute shader test") {
    val storageSizeX = 256
    val storageSizeY = 256

    // a compute shader always has a single compute stage
    computeStage(16, 16, 1) {
        // storage maps to a 1d / 2d / 3d array of 1d / 2d / 4d float or (u)int vectors
        // here we use a 2d array of 4d floats
        val pixelStorage = storage2d<KslFloat4>("pixelStorage", storageSizeX, storageSizeY)

        // regular uniforms...
        val offsetPos = uniformFloat2("uOffset")

        // compute program
        main {
            // builtin globals provide information about local / global invocation IDs, work
            // group ID, count and size
            val numInvocationsXy = float2Var((inNumWorkGroups.xy * inWorkGroupSize.xy).toFloat2())
            val texelCoord = int2Var(inGlobalInvocationId.xy.toInt2())

            // for this demo we simply generate a position / workgroup dependent color
            val pos = float2Var((texelCoord.toFloat2() / numInvocationsXy + sin(offsetPos)) * 0.5f.const)
            val rgba = float4Var(Color.BLACK.const)
            rgba.r set pos.x
            rgba.g set pos.y
            rgba.b set 1f.const - pos.x
            `if` (inWorkGroupId.x.toInt1() % 2.const eq inWorkGroupId.y.toInt1() % 2.const) {
                rgba.r set 1f.const - rgba.r
                rgba.g set 1f.const - rgba.g
                rgba.b set 1f.const - rgba.b
            }

            // storage textures can be randomly read and written
            pixelStorage[texelCoord] = rgba
        }
    }
}
