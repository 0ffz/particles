package me.dvyy.particles.ui.composables

import androidx.compose.runtime.Composable
import de.fabmax.kool.modules.compose.composables.rendering.Image
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.align
import de.fabmax.kool.modules.compose.modifiers.clickable
import de.fabmax.kool.modules.compose.modifiers.padding
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.CircularBackground
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color

@Composable
fun IconButton(icon: Texture2d, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Image(
        icon,
        modifier = modifier.clickable(
            hoverBackground = CircularBackground(Color.WHITE.withAlpha(0.2f))
        ) {
            onClick()
        }.padding(4.dp).align(AlignmentX.Center, AlignmentY.Center)
    )
}
