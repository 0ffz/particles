package me.dvyy.particles.ui.sidebar

import de.fabmax.kool.modules.compose.surface.ComposableSurface
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.util.MdColor
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun uiModule() = module {
    single { ParticlesUI() }
    single(named("ui-scene")) { get<ParticlesUI>().scene }
}
val colors = Colors.singleColorDark(MdColor.LIGHT_BLUE).run {
    copy(background = background.withAlpha(0.9f))
}

class ParticlesUI {
    val surface = ComposableSurface(
        colors = colors,
        sizes = Sizes.large,
    ) {
        Sidebar()
    }

    val scene = UiScene {
        addNode(surface)
    }
}
