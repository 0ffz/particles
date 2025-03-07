package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslExpression
import de.fabmax.kool.modules.ksl.lang.KslFloat1
import de.fabmax.kool.modules.ksl.lang.KslScopeBuilder
import de.fabmax.kool.modules.ksl.lang.functionFloat1
import kotlin.jvm.JvmName

class KslPairwiseFunction(
    stage: KslComputeStage,
    name: String,
) {
    @PublishedApi
    internal val function = stage.functionFloat1(name) { }
    val distance = function.paramFloat1("dist")
    val maxForce = function.paramFloat1("maxForce")

    @JvmName("asShaderParamFloat")
    fun FunctionParameter<Float>.asShaderParam() = function.paramFloat1(name)
    @JvmName("asShaderParamInt")
    fun FunctionParameter<Int>.asShaderParam() = function.paramInt1(name)

    inline fun body(crossinline apply: KslScopeBuilder.() -> KslExpression<KslFloat1>) = function.body {
        apply()
    }
}
