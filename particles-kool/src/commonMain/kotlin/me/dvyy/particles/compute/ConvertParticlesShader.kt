package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*

class ConvertParticlesShader {
    val shader = KslComputeShader("Fields") {
        computeStage(WORK_GROUP_SIZE) {
            val convertChances = storage1d<KslFloat1>("convertChances")
            val convertTo = storage1d<KslInt1>("convertTo")
            val particleTypes = storage1d<KslInt1>("particleTypes")
            val randomSeed = uniformFloat1("randomSeed")
            val randomF = functionFloat1("randomF") {
                val seed = paramFloat1("seed")

                body {
                    val x = float1Var(seed)
//                    x *= x + 33.3f.const
//                    x *= x + x
                    fract(sin(x) * 43758.5453.const)
                }
            }

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val type = int1Var(particleTypes[id])
                val chance = float1Var(convertChances[type])
                `if`((randomF(id.toFloat1() + randomSeed)) lt chance) {
                    particleTypes[id] = convertTo[type]
                }
            }
        }
    }

    var convertChances by shader.storage1d("convertChances")
    var convertTo by shader.storage1d("convertTo")
    var particleTypes by shader.storage1d("particleTypes")
    var randomSeed by shader.uniform1f("randomSeed")
}
