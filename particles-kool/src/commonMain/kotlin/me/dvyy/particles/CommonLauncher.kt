package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.debugOverlay
import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.ui.AppSizes
import me.dvyy.particles.ui.helpers.TRANSPARENT

fun launchApp(ctx: KoolContext, forces: List<Force>) {
    val baseModule = persistentModule(ctx)

    val manager = SceneManager(ctx, baseModule, forces)

    ctx.scenes += debugOverlay()
    ctx.scenes += UiScene("version-info") {
        addPanelSurface(
            sizes = Sizes.small,
            colors = Colors.darkColors(primary = Color("b2ff00"), background = Color.TRANSPARENT),
            width = FitContent,
            height = FitContent
        ) {
            modifier.align(AlignmentX.End, AlignmentY.Bottom)
                .padding(vertical = 1.dp)
                .padding(end = AppSizes.sidebarSize + 1.dp)
            Text("v${BuildKonfig.version}") {
                modifier
                    .align(AlignmentX.End, AlignmentY.Bottom)
                    .textColor(Color.WHITE.withAlpha(0.6f))
            }
        }
    }
    manager.load()
}

