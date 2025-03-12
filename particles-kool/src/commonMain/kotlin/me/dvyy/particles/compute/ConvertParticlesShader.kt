package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.util.Time
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.Buffers

class ConvertParticlesShader(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
) {
    val shader = KslComputeShader("Fields") {
        computeStage(WORK_GROUP_SIZE) {
            val convertChances = storage<KslFloat1>("convertChances")
            val convertTo = storage<KslInt1>("convertTo")
            val particleTypes = storage<KslInt1>("particleTypes")
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

    var convertChances by shader.storage("convertChances")
    var convertTo by shader.storage("convertTo")
    var particleTypes by shader.storage("particleTypes")
    var randomSeed by shader.uniform1f("randomSeed")

    fun addTo(computePass: ComputePass) {
        val config = configRepo.config.value
        computePass.addTask(shader, numGroups = configRepo.numGroups).apply {

            val conversionBuffer = Buffers.integers(config.particles.size)
            val conversionChances = Buffers.floats(config.particles.size)
            config.particles.forEachIndexed { id, from ->
                val to = from.convertTo
                if (to != null) {
                    //TODO
//                    conversionBuffer[id] = config.particleIds[to.type]!!.id.toInt()
//                    conversionChances[id] = to.chance.toFloat()
                }
            }
            convertTo = conversionBuffer
            convertChances = conversionChances
            particleTypes = buffers.particleTypesBuffer
            onBeforeDispatch {
                val count = configRepo.count
                val conversionRate = configRepo.config.value.simulation.conversionRate
                val shouldRun = Time.frameCount % conversionRate == 0
                setNumGroupsByInvocations(if (shouldRun) configRepo.count else 0, 1, 1)
                randomSeed = count + (Time.gameTime % count).toFloat()
            }
        }
    }
}
