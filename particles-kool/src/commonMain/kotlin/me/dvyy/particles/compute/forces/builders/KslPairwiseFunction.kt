package me.dvyy.particles.compute.forces.builders

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslExpression
import de.fabmax.kool.modules.ksl.lang.KslFloat1
import de.fabmax.kool.modules.ksl.lang.KslFloat3
import de.fabmax.kool.modules.ksl.lang.KslFunction
import de.fabmax.kool.modules.ksl.lang.KslScopeBuilder
import de.fabmax.kool.modules.ksl.lang.functionFloat1
import de.fabmax.kool.modules.ksl.lang.functionFloat2
import de.fabmax.kool.modules.ksl.lang.functionFloat3
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
    val maxForce = function.paramFloat1("maxForce")

    inline fun body(crossinline apply: KslScopeBuilder.() -> KslExpression<KslFloat1>) = function.body {
        apply()
    }
}
