package me.dvyy.particles.compute.helpers

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE

abstract class CustomShader(name: String) : KslComputeShader(name) {
    abstract fun KslComputeStage.computeStage()

    init {
        program.computeStage(WORK_GROUP_SIZE) {
            computeStage()
        }
    }
}
