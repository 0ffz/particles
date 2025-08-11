package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.Grow
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.util.Color
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.Subcategory
import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.helpers.TRANSPARENT
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ForceParametersViewModel

@Composable
fun LiveParametersWindow(
    configRepo: ConfigRepository = koinInject(),
    forceParametersViewModel: ForceParametersViewModel = koinInject(),
    configViewModel: ConfigViewModel = koinInject(),
) {
    Category("Simulation") {
        val simulation by configViewModel.simulation.collectAsState()
        Text(simulation.dT.toString())
        MenuNumber("dT", simulation.dT.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(dT = it.toDouble()) }
        })
        MenuNumber("Max Velocity", simulation.maxVelocity.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(maxVelocity = it.toDouble()) }
        })
        MenuNumber("Max Force", simulation.maxForce.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(maxForce = it.toDouble()) }
        })

        ResetSubcategory()
    }

    Category("Interactions") {
        val parameters by forceParametersViewModel.parameters.collectAsState(arrayOf())
        parameters.forEach { force ->
            Subcategory(force.name) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Pair")
                        force.interactions.forEachIndexed { row, interaction ->
                            val bg = if (row % 2 == 0) Color.WHITE.withAlpha(0.1f) else Color.TRANSPARENT
                            Box(Modifier.fillMaxWidth().backgroundColor(bg)) {
                                Row {
                                    interaction.set.ids.forEachIndexed { i, it ->
                                        val particle = configRepo.config.value.particles[it.id]
                                        val name = configRepo.config.value.particleName(it)
                                        //TODO label style
                                        Text(name, color = Color(particle.color).mix(Color.WHITE, 0.7f))
                                        if (i != interaction.set.ids.lastIndex)
                                            Text("-", color = Color.LIGHT_GRAY)
                                    }
                                }
                            }
                        }
                    }
                    force.interactions.firstOrNull()?.parameters?.indices?.forEach { i ->
                        Column {
                            Text(force.interactions.first().parameters[i].name, Modifier.padding(horizontal = 8.dp))
                            force.interactions.forEachIndexed { row, interaction ->
                                val bg = if (row % 2 == 0) Color.WHITE.withAlpha(0.1f)
                                else Color.TRANSPARENT
                                Box(Modifier.width(Grow.MinFit).padding(horizontal = 8.dp).backgroundColor(bg)) {
                                    val parameter = interaction.parameters[i]
                                    TextInputWithTooltip(
                                        parameter.value,
                                        modifier = Modifier.width(Grow.MinFit),
                                        onValueChange = { new ->
                                            forceParametersViewModel.updateParameter(
                                                force = force.name,
                                                interaction = interaction.set,
                                                name = parameter.name,
                                                value = new.toFloat()
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
