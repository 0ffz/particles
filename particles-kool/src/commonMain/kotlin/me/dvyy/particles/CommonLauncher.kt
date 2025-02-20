package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.debugOverlay
import de.fabmax.kool.util.delayFrames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.ui.AppUI
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
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

class SceneManager(
    val ctx: KoolContext,
    val baseModule: Module,
) {
    private var loadedScenes: List<Scene> = listOf()

    suspend fun reload() {
        unload()
        delayFrames(1)
        load()
    }
    fun load() {
        val sceneScope = CoroutineScope(Dispatchers.RenderLoop)
        val application = koinApplication {
            modules(
                module {
                    single { sceneScope }
                    single<SceneManager> { this@SceneManager }
                },
                baseModule,
                sceneModule(),
//                gpuModule()
            )
        }
        application.koin.get<ConfigRepository>().isDirty = true
        val ui = application.koin.get<AppUI>().ui
        val scene = application.koin.get<ParticlesScene>().scene

        scene.onRelease { sceneScope.cancel() }
        ctx.scenes.stageAdd(scene, index = 0)
        ctx.scenes +=ui
        loadedScenes = listOf(scene, ui)
    }

    fun unload() {
        loadedScenes.forEach {
            ctx.removeScene(it)
            it.release()
        }
        loadedScenes = listOf()
    }
}
