package me.dvyy.particles.ui.sidebar

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.ReverseRowLayout
import de.fabmax.kool.modules.ui2.RowLayout
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.sidebar.AppSizes.sidebarSize
import me.dvyy.particles.ui.windows.Window

object AppSizes {
    val sidebarSize = 32.dp
}

data class WindowUiState(
    val title: String,
    val icon: Texture2d,
    val content: @Composable () -> Unit,
)

@Composable
fun Sidebar(
    tabs: List<WindowUiState>,
    rightAligned: Boolean = false,
) {
    var state by remember { mutableStateOf(SidebarUiState(-1, 350.0)) }

    fun selectOrClose(value: Int) {
        state = if (state.selectedTab == value)
            state.copy(selectedTab = -1)
        else state.copy(selectedTab = value)
    }

    Row(
        Modifier.fillMaxHeight()
            .layout(if (rightAligned) ReverseRowLayout else RowLayout)
            .alignX(if (rightAligned) AlignmentX.End else AlignmentX.Start)
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(sidebarSize)
                .backgroundColor(Color.BLACK.withAlpha(0.7f))
        ) {
            tabs.forEachIndexed { i, window ->
                SidebarIcon(onClick = {
                    selectOrClose(i)
                }, isSelected = state.selectedTab == i, icon = window.icon)
            }
        }
        tabs.getOrNull(state.selectedTab)?.let { window ->
            Window(
                title = window.title,
                rightAligned = rightAligned,
                onDeltaResize = { state = state.copy(windowSize = state.windowSize + it.value) },
                modifier = Modifier.width(state.windowSize.dp)
            ) {
                window.content()
            }
        }
    }
}
