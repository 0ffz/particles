package me.dvyy.particles.ui.viewmodels

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.dvyy.particles.compute.forces.ForcesDefinition
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.dsl.pairwise.ParticleSet

data class ForceUiState(
    val name: String,
    val interactions: List<InteractionUiState>,
)

data class InteractionUiState(
    val name: String,
    val set: ParticleSet,
    val parameters: List<UniformParameter>,
)

class ForceParametersViewModel(
    val forcesDefinition: ForcesDefinition,
    val config: ConfigRepository,
) {
    val parameters = combine(forcesDefinition.forces.map { force ->
        force.changes.map {
            println("Changes made to $force!")
            ForceUiState(
                name = force.force.name,
                interactions = force.getAll().map { (set, values) ->
                    InteractionUiState(
                        name = set.ids.joinToString("-") { config.config.value.particleName(it) },
                        set = set,
                        parameters = values.mapIndexed { i, value ->
                            UniformParameter(
                                name = force.parameterNames[i],
                                configPath = "todo",
                                value = value,
                            )
                        }
                    )
                }
            )
        }
    }) { it }

    fun updateParameter(
        force: String,
        interaction: ParticleSet,
        name: String,
        value: Float,
    ) {
        forcesDefinition.forces
            .find { it.force.name == force }
            ?.update(interaction, name, value)
    }
}
