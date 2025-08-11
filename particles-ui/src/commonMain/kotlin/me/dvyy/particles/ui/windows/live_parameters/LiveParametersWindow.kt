package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.rendering.Text
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.helpers.koinInject

@Composable
fun LiveParametersWindow(
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
}
