package me.dvyy.particles.ui.windows

import androidx.compose.runtime.*
import de.fabmax.kool.input.CursorShape.RESIZE_EW
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.toolkit.ScrollArea
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.composables.modifiers.hoverCursor

@Composable
fun Window(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Box(Modifier.fillMaxHeight()) {
    var width by remember { mutableStateOf(200.dp) }

    // Content
    ScrollArea(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .backgroundColor(Color.BLACK.withAlpha(0.6f))
                .width(width)
                .fillMaxHeight()
                .then(modifier)
        ) {
            content()
        }
    }

    // Resize handle
    Box(
        Modifier.fillMaxHeight().width(4.dp)
            .alignX(AlignmentX.End)
            .hoverCursor(shape = RESIZE_EW)
            .onDrag {
                width += it.pointer.delta.x.dp
            }
    ) {}
}

