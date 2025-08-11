package me.dvyy.particles.ui.windows

import androidx.compose.runtime.Composable
import de.fabmax.kool.input.CursorShape.RESIZE_EW
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.ScrollArea
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.dp
import me.dvyy.particles.ui.composables.modifiers.hoverCursor

@Composable
fun WindowTitle(title: String) = Box(
    Modifier.fillMaxWidth()
        .padding(4.dp)
        .backgroundColor(LocalColors.current.primaryVariant)
) {
    Text(title)
}

@Composable
fun Window(
    title: String,
    modifier: Modifier = Modifier,
    rightAligned: Boolean = false,
    onDeltaResize: (Dp) -> Unit = {},
    content: @Composable () -> Unit,
) = Box(Modifier.fillMaxHeight()) {

    // Content
    ScrollArea(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .backgroundColor(LocalColors.current.background)
                .fillMaxHeight()
                .then(modifier)
        ) {
            WindowTitle(title)
            content()
        }
    }

    // Resize handle
    Box(
        Modifier.fillMaxHeight()
            .width(4.dp)
            .alignX(if (rightAligned) AlignmentX.Start else AlignmentX.End)
            .hoverCursor(shape = RESIZE_EW)
            .onDrag {
                onDeltaResize(if (rightAligned) (-it.pointer.delta.x).dp else it.pointer.delta.x.dp)
            }
    ) {}
}

