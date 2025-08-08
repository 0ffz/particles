package me.dvyy.particles.ui.windows.project_switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.dp
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.composables.IconButton
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

@Composable
fun ProjectSwitcherWindow(
    viewModel: ParticlesViewModel = koinInject(),
    settings: AppSettings = koinInject(),
) {
    Button(onClick = { viewModel.attemptOpenProject() }) {
        Text("Open project")
    }
    Column(Modifier.fillMaxWidth()) {
        val recentPaths by settings.recentProjectPaths.collectAsState()
        recentPaths.forEach { path ->
            ProjectButton(
                path = path,
                onOpen = { viewModel.openProject(path) },
//                    it.isLeftClick -> viewModel.openProject(path)
//                    it.pointer.isMiddleButtonClicked -> viewModel.removeProject(path)
                onRemove = { viewModel.removeProject(path) }
            )
        }
    }
}

@Composable
private fun ProjectButton(
    path: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
        .clickable { onOpen() }
        .background(RoundRectBackground(LocalColors.current.backgroundVariant, 4.dp))
    ) {
        Text(path.substringAfterLast("/").substringBeforeLast("."), Modifier.fillMaxWidth().padding(4.dp))
        IconButton(Icons.x, onClick = { onRemove() })
    }

}
