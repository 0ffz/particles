package me.dvyy.particles.compute.forces.builders

import de.fabmax.kool.modules.ksl.lang.*
import kotlin.jvm.JvmName

abstract class KslForceFocuntion(
    stage: KslComputeStage,
    name: String,
) {
    internal abstract val function: KslFunction<*>

    @JvmName("asShaderParamFloat")
    fun FunctionParameter<Float>.asShaderParam() = function.paramFloat1(name)
    @JvmName("asShaderParamInt")
    fun FunctionParameter<Int>.asShaderParam() = function.paramInt1(name)
}

class KslIndividualForceFunction(
    stage: KslComputeStage,
    name: String,
): KslForceFocuntion(stage, name) {
    @PublishedApi
    override val function = stage.functionFloat3(name) {  }

    val position = function.paramFloat3("position")

    inline fun body(crossinline apply: KslScopeBuilder.() -> KslExpression<KslFloat3>) = function.body {
        apply()
    }
}
class KslPairwiseFunction(
    stage: KslComputeStage,
    name: String,
): KslForceFocuntion(stage, name) {
    @PublishedApi
    override val function = stage.functionFloat1(name) { }

    val distance = function.paramFloat1("dist")
    val localCount = function.paramFloat1("localCount")

    inline fun body(crossinline apply: KslScopeBuilder.() -> KslExpression<KslFloat1>) = function.body {
        apply()
    }
}
