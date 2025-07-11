package me.dvyy.particles.forces.pairwise

import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.forces.builders.KslPairwiseFunction

object TersoffSimple : PairwiseForce("tersoff_simple") {
    val A = param<Float>("A")
    val B = param<Float>("B")
    val lambda1 = param<Float>("lambda1")
    val lambda2 = param<Float>("lambda2")
    val beta = param<Float>("beta")
    val n = param<Float>("n")

//    val KslComputeStage.cutoffFunction
//        get() = functionFloat1("tersoff_cutoff") {
//            paramFloat1("distance")
//            body {
//                1f.const //TODO implement
//            }
//        }
//    forceBetweenParticles += -tersoff(
//    localCount = localCount,
//    distance = dist,
//    beta = ,
//    n = ,
//    A = ,
//    B = ,
//    lambda_1 = ,
//    lambda_2 = ,
//    )

    // We assume a_ij = 1, as suggested by Tersoff

    fun KslScopeBuilder.`b_ij`(
        localCount: FloatParam,
        beta: FloatParam,
        n: FloatParam,
    ) = pow(1f.const + pow(beta, n) * pow(localCount, n), ((-1f).const / (2f.const * n)))

    fun KslScopeBuilder.force(
        scalar: FloatParam,
        lambda: FloatParam,
        distance: FloatParam,
    ) = scalar * exp(-lambda * distance)

    fun KslScopeBuilder.d_force(
        scalar: FloatParam,
        lambda: FloatParam,
        distance: FloatParam,
    ) = - (scalar * lambda) * exp(-lambda * distance)

    // assuming: cutoff is just 1, b_ij is constant
    override fun KslPairwiseFunction.createFunction() {
        //TODO ability to derive shader params based on parts (to avoid wasting compute on repeated calculations)
        val A = A.asShaderParam()
        val B = B.asShaderParam()
        val lambda1 = lambda1.asShaderParam()
        val lambda2 = lambda2.asShaderParam()
        val beta = beta.asShaderParam()
        val n = n.asShaderParam()

        body {
            val d_repulsive = d_force(A, lambda1, distance)
            val d_attractive = d_force(-B, lambda2, distance)
            -(d_repulsive + (b_ij(localCount, beta, n) * d_attractive))
        }
    }

}

typealias FloatParam = KslScalarExpression<KslFloat1>
