package me.dvyy.particles.ui.sidebar

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.ReverseRowLayout
import de.fabmax.kool.modules.ui2.RowLayout
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.sidebar.AppSizes.sidebarSize

object AppSizes {
    val sidebarSize = Dp(34f)
}

data class WindowUiState(
    val icon: Texture2d,
    val content: @Composable () -> Unit,
)

@Composable
fun Sidebar(
    state: List<WindowUiState>,
    rightAligned: Boolean = false,
) {
    var selected by remember { mutableStateOf(-1) }

    fun selectOrClose(value: Int) = if (selected == value) selected = -1 else selected = value

    Row(
        Modifier.fillMaxHeight()
            .layout(if (rightAligned) ReverseRowLayout else RowLayout)
            .alignX(if(rightAligned) AlignmentX.End else AlignmentX.Start)
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(sidebarSize)
                .backgroundColor(Color.BLACK.withAlpha(0.7f))
        ) {
            state.forEachIndexed { i, window ->
                SidebarIcon(onClick = {
                    selectOrClose(i)
                }, isSelected = selected == i, icon = window.icon)
            }
        }
        state.getOrNull(selected)?.content?.invoke()
    }
}
