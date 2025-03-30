package me.dvyy.particles.ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.launchDelayed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.render.UiScale
import me.dvyy.particles.ui.AppSizes.sidebarSize
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.SimulationStatisticsWindow
import me.dvyy.particles.ui.windows.TextEditorWindow
import me.dvyy.particles.ui.windows.UniformsWindow
import me.dvyy.particles.ui.windows.VisualOptionsWindow

object AppSizes {
    val sidebarSize = Dp(32f)
}

class AppUI(
    val ctx: KoolContext,
    val uniforms: UniformParameters,
    val viewModel: ParticlesViewModel,
    val configRepository: ConfigRepository,
    val settings: AppSettings,
    val scope: CoroutineScope,
) {
    val uiSizes = mutableStateOf(Sizes.large)
    val colors = Colors.singleColorDark(MdColor.LIGHT_BLUE).run {
        copy(background = background.withAlpha(0.9f))
    }
    val dock = Dock("Dock")

    val uniformsWindow = UniformsWindow(this@AppUI, viewModel, configRepository, uniforms, scope)
    val textEditorWindow = TextEditorWindow(this@AppUI, configRepository, viewModel, scope)
    val statsWindow = SimulationStatisticsWindow(this@AppUI, viewModel, configRepository)
    val visualsWindow = VisualOptionsWindow(this@AppUI, viewModel, settings, scope)

    val ui = UiScene {
        dock.dockingSurface.colors = colors
        dock.dockingSurfaceOverlay.colors = colors
        dock.dockingPaneComposable = Composable {
            Row(Grow.Std, Grow.Std) {
                modifier.margin(horizontal = sidebarSize)
                dock.root()
            }
        }

        addPanelSurface {
            surface.sizes = this@AppUI.uiSizes.use()
            surface.colors = this@AppUI.colors
            modifier.height(Grow.Std).backgroundColor(colors.background)
            Box {
                modifier.width(sidebarSize)
                windowSelector(listOf(uniformsWindow, textEditorWindow), "0:row/0:leaf")
            }
        }
        addPanelSurface {
            surface.sizes = this@AppUI.uiSizes.use()
            surface.colors = this@AppUI.colors
            modifier.height(Grow.Std).backgroundColor(colors.background)
                .alignX(AlignmentX.End)
            Box {
                modifier.width(sidebarSize)
                windowSelector(listOf(statsWindow, visualsWindow), "0:row/2:leaf")
            }
        }
        addNode(dock)

        dock.createNodeLayout(
            listOf(
                "0:row",
                "0:row/0:leaf",
                "0:row/1:leaf",
                "0:row/2:leaf"
            )
        )
        val centerSpacer = UiDockable("EmptyDockable", dock, isHidden = true)
        dock.getLeafAtPath("0:row/1:leaf")?.dock(centerSpacer)
    }

    init {
        scope.launch { settings.ui.scale.collect { uiSizes.set(it.size) } }
    }

    private val windowSpawnLocation = MutableVec2f(32f, 32f)

    fun spawnWindow(window: FieldsWindow, dockPath: String? = null) {
        dock.addDockableSurface(window.windowDockable, window.windowSurface)
        dockPath?.let {
            val leaf = dock.getLeafAtPath(it)
            leaf?.dock(window.windowDockable)
            if(leaf?.width?.value !is Dp && window.preferredWidth != null)
                leaf?.width?.set(Dp(window.preferredWidth))
            return
        }

        window.windowDockable.setFloatingBounds(
            Dp(windowSpawnLocation.x), Dp(windowSpawnLocation.y),
        )
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

    fun UiScope.windowSelector(windows: List<FieldsWindow>, nodePath: String) {
        val selected = remember<Int?>(0)
        MultiSelect(windows, selected.use(), onSelect = {
            val previous = selected.value
            if (previous != null) {
                val prevWindow = windows[previous]
                dock.getLeafAtPath(nodePath)?.undock(prevWindow.windowDockable, removeIfEmpty = false)
                dock.removeDockableSurface(prevWindow.windowSurface)
            }
            if (it != null) {
                spawnWindow(windows[it], nodePath)
            }
            selected.set(it)
        })
    }
}


fun UiScope.MultiSelect(
    windows: List<FieldsWindow>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    remember { onSelect(selected) }
    Column {
        windows.forEachIndexed { index, window ->
            SquareButton(selected == index, window.icon) {
                onSelect(index.takeIf { selected != it })
            }
        }
    }
}

fun UiScope.SquareButton(
    selected: Boolean,
    icon: Texture2d?,
    tint: Color = colors.onBackground,
    onClick: () -> Unit
) {
    val color = if (selected) colors.primaryVariant else Color.BLACK.withAlpha(0f)
    Box {
        modifier.size(sidebarSize, sidebarSize)
        Button {
            modifier
                .size(Grow.Std, Grow.Std)
                .margin(2.dp)
                .padding(2.dp)
                .colors(buttonColor = color, buttonHoverColor = colors.primary)
                .onClick { onClick() }
            if (icon != null) Image(icon) {
                modifier.align(AlignmentX.Center, AlignmentY.Center)
                    .image(icon)
                    .tint(tint)
            }
        }
    }
}
