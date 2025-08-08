package me.dvyy.particles.ui.composables

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.LocalSizes
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentX.Center
import de.fabmax.kool.modules.ui2.Grow.Companion.MinFit
import de.fabmax.kool.util.MsdfFont

@Composable
fun Category(
    name: String,
    desc: String? = null,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val colors = LocalColors.current
    val sizes = LocalSizes.current

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(MinFit)
                .backgroundColor(colors.primaryVariant.withAlpha(0.2f))
                .padding(vertical = sizes.smallGap)
                .clickable { expanded = !expanded }
        ) {
            Text(name, Modifier.alignX(Center), color = colors.primary, font = MsdfFont.DEFAULT_FONT, fontSize = 24f)
        }
        if (desc != null) Text(
            "*$desc",
            color = colors.onBackgroundAlpha(0.5f),
        )
        if (expanded) content()
    }
}

@Composable
fun SubCategory(
    name: String,
    content: @Composable () -> Unit,
) {
    val sizes = LocalSizes.current
    val colors = LocalColors.current
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth()
                .padding(vertical = sizes.smallGap)
                .backgroundColor(LocalColors.current.primaryVariant.withAlpha(0.1f))
        ) {

            Text(
                name,
                color = colors.primary,
                font = sizes.normalText,
                modifier = Modifier.alignX(Center)
            )
        }
        content()
    }
}
