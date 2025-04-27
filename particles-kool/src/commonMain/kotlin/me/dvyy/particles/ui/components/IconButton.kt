package me.dvyy.particles.ui.components

import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Box
import de.fabmax.kool.modules.ui2.Button
import de.fabmax.kool.modules.ui2.ButtonScope
import de.fabmax.kool.modules.ui2.CircularBackground
import de.fabmax.kool.modules.ui2.Hoverable
import de.fabmax.kool.modules.ui2.Image
import de.fabmax.kool.modules.ui2.PointerEvent
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.backgroundColor
import de.fabmax.kool.modules.ui2.colors
import de.fabmax.kool.modules.ui2.hoverListener
import de.fabmax.kool.modules.ui2.image
import de.fabmax.kool.modules.ui2.margin
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.modules.ui2.padding
import de.fabmax.kool.modules.ui2.remember
import de.fabmax.kool.modules.ui2.tint
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import me.dvyy.particles.ui.helpers.TRANSPARENT

fun UiScope.IconButton(
    icon: Texture2d?,
    tint: Color = colors.onBackground,
    onClick: (PointerEvent) -> Unit,
    content: UiScope.() -> Unit = {},
) {
    Box {
        var hovered by remember(false)
        modifier.hoverListener(object: Hoverable {
            override fun onEnter(ev: PointerEvent) {
                hovered = true
            }

            override fun onExit(ev: PointerEvent) {
                hovered = false
            }
        }).background(CircularBackground(if(hovered) Color.WHITE.withAlpha(0.1f) else Color.TRANSPARENT))
            .onClick(onClick)
            .padding(4.dp)
            .margin(1.dp)
//        modifier.colors(
//            buttonColor = Color.Companion.TRANSPARENT,
//            buttonHoverColor = Color.Companion.WHITE.withAlpha(0.1f)
//        ).onClick { onClick(it) }

        Image(icon) {
            modifier.align(AlignmentX.Center, AlignmentY.Center)
                .image(icon)
                .tint(tint)
        }
        content()
    }
}
