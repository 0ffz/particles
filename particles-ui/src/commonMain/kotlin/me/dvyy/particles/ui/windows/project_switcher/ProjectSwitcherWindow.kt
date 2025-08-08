package me.dvyy.particles.ui.windows.project_switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Image
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.clickable
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import de.fabmax.kool.pipeline.Texture2d
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.Window

@Composable
fun ProjectSwitcherWindow(
    viewModel: ParticlesViewModel = koinInject(),
    settings: AppSettings = koinInject(),
) = Window {
    Button(onClick = { viewModel.attemptOpenProject() }) {
        Text("Open project")
    }
    Column(Modifier.fillMaxWidth()) {
        val recentPaths by settings.recentProjectPaths.collectAsState()
        recentPaths.forEachIndexed { index, path ->
            Row(Modifier.fillMaxWidth()) {
                Button(onClick = {
                    viewModel.openProject(path)
//                when {
//                    it.isLeftClick -> viewModel.openProject(path)
//                    it.pointer.isMiddleButtonClicked -> viewModel.removeProject(path)
                }, Modifier.fillMaxWidth()) {
                    Text(path)
                }
                IconButton(Icons.x, onClick = {
                    viewModel.removeProject(path)
                })
            }
        }
    }
}


@Composable
fun IconButton(icon: Texture2d, onClick: () -> Unit) {
    Image(icon, modifier = Modifier.clickable {
        onClick()
    })
}
