package me.dvyy.particles.ui.sidebar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.input.CursorShape
import de.fabmax.kool.input.CursorShape.RESIZE_EW
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.modules.compose.Colors
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Image
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.composables.toolkit.TextField
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.toString
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.TRANSPARENT
import me.dvyy.particles.ui.sidebar.AppSizes.sidebarSize

object AppSizes {
    val sidebarSize = Dp(34f)
}

@Composable
fun Sidebar() {
    var selected by remember { mutableStateOf(-1) }

    fun selectOrClose(value: Int) = if (selected == value) selected = -1 else selected = value
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(sidebarSize)
                .backgroundColor(Color.BLACK.withAlpha(0.7f))
        ) {
            SidebarIcon(onClick = {
                selectOrClose(0)
            }, isSelected = selected == 0, icon = Icons.eye)
            SidebarIcon(onClick = {
                selectOrClose(1)
            }, isSelected = selected == 1, icon = Icons.settings)
        }
        if (selected != -1) Window()
    }
}

class TestViewModel(
) {
    val fps = flow<String> {
        while(true) {
            emit(KoolSystem.requireContext().fps.toString(2))
            yield()
        }
    }.flowOn(Dispatchers.RenderLoop)
}

@Composable
fun Window() = Box(Modifier.fillMaxHeight()) {
    var width by remember { mutableStateOf(200.dp) }
    val viewModel = remember { TestViewModel() }
    Column(
        Modifier
            .backgroundColor(Color.BLACK.withAlpha(0.6f))
            .width(width)
            .fillMaxHeight()
    ) {
        val fps by viewModel.fps.collectAsState("0")
        var text by remember { mutableStateOf("") }
        TextField(text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        Text("Fps: $fps")
        Button(onClick = {}) {
            Text("Click me")
        }
    }
    Box(
        Modifier.fillMaxHeight().width(4.dp)
            .alignX(AlignmentX.End)
            .hoverCursor(shape = RESIZE_EW)
            .onDrag {
                width += it.pointer.delta.x.dp
            }
    ) {}
}

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

@Composable
fun SidebarIcon(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: Texture2d,
    tint: Color = WHITE,
) {
    val background =
        if (isSelected) CircularBackground(Colors.primaryVariant) else RectBackground(Color.TRANSPARENT)
    Box(
        Modifier.padding(4.dp).margin(1.dp)
            .background(background)
            .clickable(hoverBackground = CircularBackground(Color.WHITE.withAlpha(0.2f))) {
                onClick()
            }) {
        Image(icon, tint, modifier = Modifier.align(AlignmentX.Center, AlignmentY.Center))
    }
}
