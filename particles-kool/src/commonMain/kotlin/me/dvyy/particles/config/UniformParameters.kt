package me.dvyy.particles.config

import de.fabmax.kool.modules.ui2.MutableStateValue
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.dsl.pairwise.UniformParameter

class UniformParameters(
    val repo: ConfigRepository,
) {
    val uniformParams: List<Pair<UniformParameter<Float>, MutableStateValue<Float>>> =
        repo.config.value.pairwiseInteraction.flatMap { interaction ->
            //TODO generic parameter types
            interaction.uniforms.filterIsInstance<UniformParameter<Float>>().map { uniform ->
                uniform to repo.parameters.get<Float>(
                    uniform.parameter.path,
                    default = uniform.parameter.default,
                    serializer = Float.serializer() //TODO uniform.parameter.serializer
                )
            }
        }
}
