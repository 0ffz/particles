package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * A force between two particles, given only distance as a parameter
 */
abstract class PairwiseForce(val name: String) {
    @PublishedApi
    internal val parameters = mutableListOf<FunctionParameter<*>>()

    inline fun <reified T> param(
        name: String,
        serializer: KSerializer<T> = serializer<T>(),
    ): FunctionParameter<T> {
        val param = FunctionParameter<T>(name, serializer)
        parameters += param
        return param
    }

    abstract fun KslPairwiseFunction.createFunction()

    fun createFunction(stage: KslComputeStage) =
        KslPairwiseFunction(stage, name).apply { createFunction() }
}

