package me.dvyy.particles.ui.helpers

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.pipeline.Texture2d
import me.dvyy.particles.ui.AppUI

abstract class FieldsWindow(
    name: String,
    val ui: AppUI,
    val icon: Texture2d? = null,
    val preferredWidth: Float? = null,
    isClosable: Boolean = false
) {
    val windowDockable = UiDockable(name, ui.dock)

    val windowSurface = WindowSurface(windowDockable) {
        surface.sizes = ui.uiSizes.use()
        surface.colors = this@FieldsWindow.ui.colors

        modifyWindow()

        var isMinimizedToTitle by remember(false)
        val isDocked = windowDockable.isDocked.use()

        Column(Grow.Std, Grow.Std) {
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

    protected open fun UiScope.modifyWindow() = Unit

    abstract fun UiScope.windowContent(): Any

    open fun onClose() {}
}
