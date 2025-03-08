package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.particles.compute.ConfiguredFunction
import me.dvyy.particles.compute.forces.builders.FunctionParameter
import me.dvyy.particles.config.YamlHelpers.decode
import me.dvyy.particles.dsl.Parameter

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

