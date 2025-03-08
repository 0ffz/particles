package me.dvyy.particles.compute

import com.charleskorn.kaml.YamlNode
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.KslScalarExpression
import de.fabmax.kool.modules.ksl.lang.KslScopeBuilder
import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.compute.forces.builders.FunctionParameter
import me.dvyy.particles.config.YamlHelpers.decode
import me.dvyy.particles.dsl.InteractionConfig
import me.dvyy.particles.dsl.Parameter
import me.dvyy.particles.dsl.pairwise.ParticlePair

class ConfiguredFunction(
    val function: Force,
    val config: InteractionConfig,
) {
    val parameters = function.parameters.map { param ->
        param to (config[param.name] ?: error("Parameter ${param.name} not configured"))
    }
    val uniforms = parameters.filter {
        it.second is Parameter.FromParams
    } as List<Pair<FunctionParameter<*>, Parameter.FromParams<YamlNode>>>

    fun getParameters(
        builder: KslScopeBuilder,
        program: KslProgram,
        key: ParticlePair
    ): Array<KslScalarExpression<*>> = with(builder) {
        function.parameters.map { param ->
            val configParam = this@ConfiguredFunction.config[param.name]!!
            //TODO support more than just floats
            when (configParam) {
                is Parameter.Value -> (configParam.value.decode(param.serializer) as Float).const
                is Parameter.FromParams -> param.asUniform(program, key)
            }
        }.toTypedArray()
    }
}
