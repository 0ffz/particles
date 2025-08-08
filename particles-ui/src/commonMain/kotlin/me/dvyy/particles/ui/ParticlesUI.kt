package me.dvyy.particles.ui

import StatisticsWindow
import androidx.compose.runtime.CompositionLocalProvider
import de.fabmax.kool.modules.compose.surface.ComposableSurface
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.util.MdColor
import me.dvyy.particles.ui.helpers.LocalKoinScope
import me.dvyy.particles.ui.sidebar.Sidebar
import me.dvyy.particles.ui.sidebar.WindowUiState
import me.dvyy.particles.ui.windows.config_editor.ConfigEditorWindow
import me.dvyy.particles.ui.windows.live_parameters.LiveParametersWindow
import me.dvyy.particles.ui.windows.project_switcher.ProjectSwitcherWindow
import me.dvyy.particles.ui.windows.visual_options.VisualOptionsWindow
import org.koin.core.scope.Scope

val colors = Colors.singleColorDark(MdColor.LIGHT_BLUE).run {
    copy(background = background.withAlpha(0.9f))
}

class ParticlesUI(
    val application: Scope,
) {
    val surface = ComposableSurface(
        colors = colors,
        sizes = Sizes.large,
    ) {
        CompositionLocalProvider(LocalKoinScope provides application) {
            Sidebar(
                listOf(
                    WindowUiState("Live Parameters", Icons.slidersHorizontal) { LiveParametersWindow() },
                    WindowUiState("Config Editor", Icons.fileCode) { ConfigEditorWindow() },
                    WindowUiState("Project Chooser", Icons.folderOpen) { ProjectSwitcherWindow() },
                ),
                rightAligned = false
            )
            Sidebar(
                listOf(
                    WindowUiState("Statistics", Icons.chartSpline) { StatisticsWindow() },
                    WindowUiState("Visual Options", Icons.eye) { VisualOptionsWindow() },
                ),
                rightAligned = true
            )
        }
    }

    val scene = UiScene {
        addNode(surface)
    }
}
