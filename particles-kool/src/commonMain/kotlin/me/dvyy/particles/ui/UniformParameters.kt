package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.MutableStateValue
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.YamlParameters
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.pairwise.UniformParameter

class UniformParameters(
    val params: YamlParameters,
    val config: ParticlesConfig,
) {
    val uniformParams: List<Pair<UniformParameter<Float>, MutableStateValue<Float>>> =
        config.pairwiseInteraction.flatMap { interaction ->
            //TODO generic parameter types
            interaction.uniforms.filterIsInstance<UniformParameter<Float>>().map { uniform ->
                uniform to params.get(
                    uniform.parameter.path,
                    default = uniform.parameter.default,
                    serializer = Float.serializer() //TODO uniform.parameter.serializer
                )
            }
        }
}
