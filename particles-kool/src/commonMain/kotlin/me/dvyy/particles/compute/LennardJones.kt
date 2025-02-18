package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.lang.*
import kotlinx.serialization.builtins.serializer

class LennardJones : PairwiseFunction() {
    val sigma = param("sigma", Float.serializer())
    override fun KslComputeStage.createFunction(): KslFunction<KslFloat1> = functionFloat1("lennardJones") {
        val dist = paramFloat1("dist")
        val maxForce = paramFloat1("maxForce")
        val sigma = paramFloat1("sigma")
        val epsilon = paramFloat1("epsilon")

        body {
            val invR = float1Var(sigma / dist)
            val invR6 = float1Var(invR * invR * invR * invR * invR * invR)
            val invR12 = float1Var(invR6 * invR6)
            min(24f.const * epsilon * (2f.const * invR12 - invR6) / dist, maxForce)
        }
    }

//    fun KslFunction<>.callFunction(program: KslScopeBuilder) = program.run {
//        parameters
//        (this@callFunction as KslFunctionInt4).invoke(1u.const)
//    }

    fun KslProgram.setupUniforms() {
        uniformFloat1("sigma")
        uniformFloat1("epsilon")
    }
}
