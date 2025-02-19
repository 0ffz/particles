package me.dvyy.particles.ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.launchDelayed
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.FieldParamsWindow

class AppUI(
    val state: AppState,
    val ctx: KoolContext,
    val uniforms: UniformParameters,
    val viewModel: ParticlesViewModel,
) {
    val colors = Colors.singleColorDark(MdColor.LIGHT_BLUE)
    val dock = Dock("Dock")
    val ui = UiScene {
        dock.dockingSurface.colors = colors
        dock.dockingSurfaceOverlay.colors = colors
        addNode(dock)

//        dock.createNodeLayout(
//            listOf(
//                "0:row",
//                "0:row/0:leaf",
//                "0:row/1:leaf",
//                "0:row/2:leaf"
//            )
//        )
//        val centerSpacer = UiDockable("EmptyDockable", dock, isHidden = true)
//        dock.getLeafAtPath("0:row/1:leaf")?.dock(centerSpacer)
    }

    private val windowSpawnLocation = MutableVec2f(32f, 32f)

    init {
        spawnWindow(FieldParamsWindow(this@AppUI, viewModel, uniforms))
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
