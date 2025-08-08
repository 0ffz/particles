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
import me.dvyy.particles.ui.windows.live_parameters.LiveParametersWindow
import me.dvyy.particles.ui.windows.project_switcher.ProjectSwitcherWindow
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
                    WindowUiState(Icons.slidersHorizontal) { LiveParametersWindow() },
                    WindowUiState(Icons.folderOpen) { ProjectSwitcherWindow() },
                    WindowUiState(Icons.chartSpline) { StatisticsWindow() },
                ),
                rightAligned = false
            )
            Sidebar(
                listOf(
//                    WindowUiState(Icons.chartSpline) { StatisticsWindow() }
                ),
                rightAligned = true
            )
        }
    }

    val scene = UiScene {
        addNode(surface)
    }
}
