package me.dvyy.particles.ui.sidebar

import androidx.compose.runtime.Composable
import de.fabmax.kool.modules.compose.Colors
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.rendering.Image
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.helpers.TRANSPARENT

@Composable
fun SidebarIcon(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: Texture2d,
    tint: Color = WHITE,
) {
    val background = if (isSelected)
        RoundRectBackground(Colors.primaryVariant, 6.dp)
    else RectBackground(Color.TRANSPARENT)
    Box(
        Modifier.padding(4.dp)
            .background(background)
            .clickable(hoverBackground = RoundRectBackground(Color.WHITE.withAlpha(0.2f), 6.dp)) {
                onClick()
            }
    ) {
        Image(icon, tint, modifier = Modifier.align(AlignmentX.Center, AlignmentY.Center))
    }
}
