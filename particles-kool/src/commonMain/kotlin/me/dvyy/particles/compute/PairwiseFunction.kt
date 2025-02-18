package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslFloat1
import de.fabmax.kool.modules.ksl.lang.KslFunction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

abstract class PairwiseFunction {
    fun <T> param(name: String, serializer: KSerializer<T>) {
//        TODO()
    }

    abstract fun KslComputeStage.createFunction(): KslFunction<KslFloat1>
    val dist = param("dist", Float.serializer())
}
