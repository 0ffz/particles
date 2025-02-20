package me.dvyy.particles.ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.launchDelayed
import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.SimulationParamsWindow
import me.dvyy.particles.ui.windows.TextEditorWindow
import me.dvyy.particles.ui.windows.UniformsWindow

class AppUI(
    val ctx: KoolContext,
    val uniforms: UniformParameters,
    val viewModel: ParticlesViewModel,
    val configRepository: ConfigRepository,
    val scope: CoroutineScope,
) {
    val colors = Colors.singleColorDark(MdColor.LIGHT_BLUE)
    val dock = Dock("Dock")
    val ui = UiScene {
        dock.dockingSurface.colors = colors
        dock.dockingSurfaceOverlay.colors = colors
        addNode(dock)

        dock.createNodeLayout(
            listOf(
                "0:row",
                "0:row/0:leaf",
                "0:row/1:leaf"
            )
        )
        val centerSpacer = UiDockable("EmptyDockable", dock, isHidden = true)
        dock.getLeafAtPath("0:row/0:leaf")?.dock(centerSpacer)
    }

    private val windowSpawnLocation = MutableVec2f(32f, 32f)

    init {
        spawnWindow(UniformsWindow(this@AppUI, viewModel, uniforms))
        spawnWindow(SimulationParamsWindow(this@AppUI, viewModel, configRepository))
        spawnWindow(TextEditorWindow(this@AppUI, configRepository, scope), "0:row/1:leaf")
    }

    fun spawnWindow(window: FieldsWindow, dockPath: String? = null) {
//        demoWindows += window

        dock.addDockableSurface(window.windowDockable, window.windowSurface)
        dockPath?.let {
            dock.getLeafAtPath(it)?.dock(window.windowDockable)
        }

        window.windowDockable.setFloatingBounds(Dp(windowSpawnLocation.x), Dp(windowSpawnLocation.y))
        windowSpawnLocation.x += 32f
        windowSpawnLocation.y += 32f
        if (windowSpawnLocation.y > 480f) {
            windowSpawnLocation.y -= 416
            windowSpawnLocation.x -= 384

            if (windowSpawnLocation.x > 480f) {
                windowSpawnLocation.x = 320f
            }
        }

        ui.apply {
            window.apply { setup(ctx) }

            launchDelayed(1) {
                window.windowSurface.isFocused.set(true)
            }
        }
    }

    fun closeWindow(window: FieldsWindow) {
        dock.removeDockableSurface(window.windowSurface)
//        demoWindows -= window
        window.onClose()
        window.windowSurface.release()
    }
}
