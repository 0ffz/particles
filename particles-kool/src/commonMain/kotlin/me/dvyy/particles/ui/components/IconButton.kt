package me.dvyy.particles.ui.components

import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Button
import de.fabmax.kool.modules.ui2.ButtonScope
import de.fabmax.kool.modules.ui2.Image
import de.fabmax.kool.modules.ui2.PointerEvent
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.colors
import de.fabmax.kool.modules.ui2.image
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.modules.ui2.tint
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.helpers.TRANSPARENT

fun UiScope.IconButton(
    icon: Texture2d,
    tint: Color = colors.onBackground,
    onClick: (PointerEvent) -> Unit,
    content: ButtonScope.() -> Unit = {},
) {
    Button {
        modifier.colors(
            buttonColor = Color.Companion.TRANSPARENT,
            buttonHoverColor = Color.Companion.WHITE.withAlpha(0.1f)
        ).onClick { onClick(it) }

        Image(icon) {
            modifier.align(AlignmentX.Center, AlignmentY.Center)
                .image(icon)
                .tint(tint)
        }
        content()
    }
}
