package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.Composable
import de.fabmax.kool.modules.compose.LocalSizes
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import de.fabmax.kool.modules.compose.modifiers.padding
import de.fabmax.kool.modules.compose.modifiers.width
import me.dvyy.particles.ui.composables.Subcategory
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.ParticlesViewModel

@Composable
fun ResetSubcategory(
    viewModel: ParticlesViewModel = koinInject(),
    paramsChanged: Boolean = false,
) = Subcategory("Reset") {
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
