package me.dvyy.particles.ui.windows.visual_options

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenu
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenuItem
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import me.dvyy.particles.render.UiScale
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.live_parameters.MenuItem

@Composable
fun VisualOptionsWindow(
    viewModel: VisualOptionsViewModel = koinInject(),
) {
    val scale = viewModel.scale.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Category(
        "FPS Calibration",
        desc = "Adjust the number of simulation steps per frame to reach a target fps. This may decrease simulation speed in favor of a smooth view."
    ) {

    }
    Category("UI") {
        MenuItem("Scale") {
            Column {
                Button(onClick = { expanded = !expanded }) {
                    Text(scale.value.name)
                }
                DropdownMenu(expanded) {
                    UiScale.entries.forEach {
                        DropdownMenuItem(Modifier.fillMaxWidth()) { Text(it.name) }
                    }
                }
            }
        }
    }
}
