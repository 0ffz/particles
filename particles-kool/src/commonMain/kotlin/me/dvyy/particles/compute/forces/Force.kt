package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.particles.compute.forces.builders.FunctionParameter
import me.dvyy.particles.compute.forces.builders.KslForceFocuntion

abstract class Force(val name: String) {
    @PublishedApi
    internal val parameters = mutableListOf<FunctionParameter<*>>()

    protected inline fun <reified T> param(
        name: String,
        serializer: KSerializer<T> = serializer<T>(),
    ): FunctionParameter<T> {
        val param = FunctionParameter<T>(name, serializer)
        parameters += param
        return param
    }

    fun parseParameters(config: Map<String, Float>): FloatArray = parameters
        .map { config[it.name] ?: error("Missing parameter ${it.name}") }
        .toFloatArray()

    /** The GPU shader code that defines this force. May require input parameters. */
    abstract fun createFunction(stage: KslComputeStage): KslForceFocuntion
}

