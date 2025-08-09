package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color

fun UiScope.SquareButton(
    selected: Boolean,
    icon: Texture2d?,
    tint: Color = colors.onBackground,
    onClick: () -> Unit,
) {
    val color = if (selected) colors.primaryVariant else Color.BLACK.withAlpha(0f)
    Box {
        modifier.size(AppSizes.sidebarSize, AppSizes.sidebarSize)
        Button {
            modifier
                .size(Grow.Std, Grow.Std)
                .margin(2.dp)
                .padding(2.dp)
                .colors(buttonColor = color, buttonHoverColor = colors.primary)
                .onClick { onClick() }
            if (icon != null) Image(icon) {
                modifier.align(AlignmentX.Center, AlignmentY.Center)
                    .image(icon)
                    .tint(tint)
            }
        }
    }
}
