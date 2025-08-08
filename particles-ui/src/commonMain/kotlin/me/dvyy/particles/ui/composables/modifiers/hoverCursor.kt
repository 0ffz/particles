package me.dvyy.particles.ui.composables.modifiers

import androidx.compose.runtime.Stable
import de.fabmax.kool.input.CursorShape
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.hoverListener
import de.fabmax.kool.modules.compose.modifiers.onDrag
import de.fabmax.kool.modules.ui2.Hoverable
import de.fabmax.kool.modules.ui2.PointerEvent

@Stable
fun Modifier.hoverCursor(shape: CursorShape = CursorShape.DEFAULT) = hoverListener(object : Hoverable {
    override fun onEnter(ev: PointerEvent) {
        PointerInput.cursorShape = shape
    }

    override fun onHover(ev: PointerEvent) {
        PointerInput.cursorShape = shape
    }
}).onDrag {
    PointerInput.cursorShape = shape
}
