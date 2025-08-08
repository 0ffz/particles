package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.LocalSizes
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.composables.toolkit.Slider
import de.fabmax.kool.modules.compose.composables.toolkit.TextField
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import de.fabmax.kool.modules.compose.modifiers.padding
import de.fabmax.kool.modules.compose.modifiers.width
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.SubCategory
import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.Window

@Composable
fun LiveParametersWindow(
    configViewModel: ConfigViewModel = koinInject(),
) = Window {
    Category("Simulation") {
        val simulation by configViewModel.simulation.collectAsState()
        Text(simulation.dT.toString())
        var text by remember { mutableStateOf("test") }
        TextField(text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        Slider(simulation.dT.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(dT = it.toDouble()) }
        }, 0f..0.01f, Modifier.fillMaxWidth())
        ResetSubcategory()
    }
}

@Composable
private fun ResetSubcategory(
    viewModel: ParticlesViewModel = koinInject(),
    paramsChanged: Boolean = false,
) = SubCategory("Reset") {
    val sizes = LocalSizes.current
    Row(Modifier.fillMaxWidth().padding(sizes.smallGap)) {
        Button(onClick = { viewModel.resetPositions() }, Modifier.fillMaxWidth()) {
            Text("Positions")
        }
        Box(Modifier.width(sizes.smallGap)) { }
        Button(onClick = { viewModel.resetParameters() }, Modifier.fillMaxWidth()) {
            Text(if (paramsChanged) "(*) Parameters" else "Parameters")
        }
    }
}
