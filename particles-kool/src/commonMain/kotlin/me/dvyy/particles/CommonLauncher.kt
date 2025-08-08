package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.util.debugOverlay
import me.dvyy.particles.compute.forces.Force
import org.koin.core.module.Module

fun launchApp(ctx: KoolContext, uiModule: () -> Module, forces: List<Force>) {
    val baseModule = persistentModule(ctx)

    val manager = SceneManager(ctx, baseModule, uiModule, forces)

    ctx.scenes += debugOverlay()
    manager.load()
}

