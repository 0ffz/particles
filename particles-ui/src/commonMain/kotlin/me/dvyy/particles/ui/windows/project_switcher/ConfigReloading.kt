package me.dvyy.particles.ui.windows.project_switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.LocalSizes
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.util.MdColor
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.ConfigUiState
import me.dvyy.particles.ui.windows.ParticlesViewModel

@Composable
fun ConfigReloading(
    particlesViewModel: ParticlesViewModel = koinInject()
) {
    val config by particlesViewModel.configUiState.collectAsState(ConfigUiState.Loading)
    val bg = if (config !is ConfigUiState.Error)
        LocalColors.current.backgroundVariant
    else (MdColor.RED tone 500).withAlpha(0.1f)

    Column(Modifier.fillMaxWidth().backgroundColor(bg).padding(4.dp)) {
        when (val config = config) {
            is ConfigUiState.Error -> {
                Text(
                    "Error loading config", font = LocalSizes.current.largeText,
                    color = MdColor.RED tone 500,
                    modifier = Modifier
                        .alignY(AlignmentY.Center)
                        .padding(vertical = 4.dp)
                )
                Text(
                    config.throwable.message.toString(),
                    Modifier.fillMaxWidth(),
                    softWrap = true
                )
            }

            else -> {}
        }
    }
}
