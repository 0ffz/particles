package me.dvyy.particles.ui.helpers

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.Column
import de.fabmax.kool.modules.ui2.Composable
import de.fabmax.kool.modules.ui2.FitContent
import de.fabmax.kool.modules.ui2.Grow
import de.fabmax.kool.modules.ui2.ScrollArea
import de.fabmax.kool.modules.ui2.TitleBar
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.WindowSurface
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.modules.ui2.remember
import de.fabmax.kool.modules.ui2.width
import de.fabmax.kool.scene.Scene
import me.dvyy.particles.ui.AppUI

abstract class FieldsWindow(name: String, val ui: AppUI, isClosable: Boolean = false) {
    val windowDockable = UiDockable(name, ui.dock)

    val windowSurface = WindowSurface(windowDockable) {
//        surface.sizes = uiDemo.selectedUiSize.use()
        surface.colors = this@FieldsWindow.ui.colors

        modifyWindow()

        var isMinimizedToTitle by remember(false)
        val isDocked = windowDockable.isDocked.use()

        Column(Grow.Companion.Std, Grow.Companion.Std) {
            TitleBar(
                windowDockable,
                isMinimizedToTitle = isMinimizedToTitle,
                onMinimizeAction = if (!isDocked && !isMinimizedToTitle) {
                    {
                        isMinimizedToTitle = true
                        windowDockable.setFloatingBounds(height = FitContent)
                    }
                } else null,
                onMaximizeAction = if (!isDocked && isMinimizedToTitle) {
                    { isMinimizedToTitle = false }
                } else null,
                onCloseAction = if (isClosable) {
                    {
                        ui.closeWindow(this@FieldsWindow)
                    }
                } else null
            )
            if (!isMinimizedToTitle) {
                windowContent()
            }
        }
    }

    open fun Scene.setup(ctx: KoolContext) { }

    protected open fun UiScope.modifyWindow() { }

    protected abstract fun UiScope.windowContent(): Any

    open fun onClose() { }
}
