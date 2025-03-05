package me.dvyy.particles.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.dvyy.particles.config.YamlHelpers.decode
import me.dvyy.particles.dsl.pairwise.UniformParameter

class UniformParameters(
    val repo: ConfigRepository,
    val scope: CoroutineScope,
) {
    val uniformParams/*: Flow<List<Pair<UniformParameter<Float>, MutableStateValue<Float>>>> */ =
        combine(repo.config, repo.parameters.overrides) { config, params ->
            config.pairwiseInteraction.flatMap { interaction ->
                //TODO generic parameter types
                interaction.uniforms.filterIsInstance<UniformParameter<Float>>().map { uniform ->
                    uniform to params[uniform.parameter.path].decode<Float>(default = uniform.parameter.default)
                }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())
//        repo.config.map {
//    }
}
