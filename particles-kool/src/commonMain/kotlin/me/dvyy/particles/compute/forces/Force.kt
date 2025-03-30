package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.particles.compute.forces.builders.FunctionParameter

abstract class Force(val name: String) {
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

    abstract fun createFunction(stage: KslComputeStage)
}

