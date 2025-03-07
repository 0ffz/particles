package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.util.debugOverlay
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.config.ConfigRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun launchApp(ctx: KoolContext, forces: List<PairwiseForce>) {
    val baseModule = module {
        single<KoolContext> { ctx }
        singleOf(::ConfigRepository)
    }

    val manager = SceneManager(ctx, baseModule, forces)

    ctx.scenes += debugOverlay()
    manager.load()
}

