package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.util.debugOverlay
import me.dvyy.particles.config.ConfigRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
//    val count: Int = (64 / 64) * 64

    val baseModule = module {
        single<KoolContext> { ctx }
        singleOf(::ConfigRepository)
    }

    val manager = SceneManager(ctx, baseModule)

    ctx.scenes += debugOverlay()
    manager.load()
}

