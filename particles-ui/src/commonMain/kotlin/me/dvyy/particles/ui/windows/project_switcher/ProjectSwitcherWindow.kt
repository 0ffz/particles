package me.dvyy.particles.ui.windows.project_switcher

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenu
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.util.Color
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.ui.app.Icons
import me.dvyy.particles.ui.composables.IconButton
import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.ParticlesViewModel

@Composable
fun ProjectSwitcherWindow(
    viewModel: ParticlesViewModel = koinInject(),
    settings: AppSettings = koinInject(),
    configViewModel: ConfigViewModel = koinInject(),
) {
    val recentPaths by settings.recentProjectPaths.collectAsState()
    ConfigReloading()
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(6.dp)) {
        val current = recentPaths.firstOrNull()
//        Text("Recent projects", Modifier.clickable { expanded = true })
        if (current != null) ProjectButton(
            current,
            removable = false,
            onOpen = { expanded = true }
        )
        DropdownMenu(expanded, modifier = Modifier.width(250.dp), onDismissRequest = { expanded = false }) {
            Button(onClick = { viewModel.attemptOpenProject() }) {
                Text("Open project")
            }
            recentPaths.forEach { path ->
                ProjectButton(
                    path = path,
                    removable = true,
                    onOpen = { viewModel.openProject(path) },
//                    it.isLeftClick -> viewModel.openProject(path)
//                    it.pointer.isMiddleButtonClicked -> viewModel.removeProject(path)
                    onRemove = { viewModel.removeProject(path) }
                )
            }
        }
    }
}

fun randomColor(hash: Int): Color {
    val hue = (hash and 0xFF) / 255f * 360f
    val saturation = 0.6f + ((hash shr 8) and 0xFF) / 255f * 0.2f
    val v = 0.8f

    return Color.Hsv(hue, saturation, v).toSrgb()
}

@Composable
private fun ProjectIcon(name: String) {
    Box(Modifier.padding(4.dp).size(32.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(RoundRectBackground(randomColor(name.hashCode()), 4.dp))
                .align(AlignmentX.Center, AlignmentY.Center)
        ) {
            Text(name.firstOrNull()?.uppercase() ?: "?", Modifier.align(AlignmentX.Center, AlignmentY.Center))
        }
    }
}

@Composable
private fun ProjectButton(
    path: String,
    removable: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onOpen() }
            .background(RoundRectBackground(LocalColors.current.backgroundVariant, 4.dp))
    ) {
        ProjectIcon(path.substringAfterLast("/"))
        Text(path.substringAfterLast("/").substringBeforeLast("."), Modifier.fillMaxWidth().padding(4.dp))
        if (removable) IconButton(Icons.x, onClick = { onRemove() })
    }
}
