package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.delayFrames
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.compute.forces.ForcesDefinition
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.ConfigPath
import me.dvyy.particles.helpers.FilePickerResult
import me.dvyy.particles.ui.AppUI
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SceneManager(
    val ctx: KoolContext,
    /** Classes/data that persists across application reloads. */
    val baseModule: Module,
    val forces: List<Force>,
) {
    private var loadedScenes: List<Scene> = listOf()
    val mainScene get() = loadedScenes.first()
    val globalApplication = koinApplication { modules(baseModule) }

    suspend fun reload() {
        unload()
        delayFrames(1)
        load()
    }

    fun load() = launchOnMainThread {
        val sceneScope = CoroutineScope(Dispatchers.RenderLoop)
        // Create dependencies with koin
        val application = koinApplication {
            modules(
                module {
                    single { sceneScope }
                    single<SceneManager> { this@SceneManager }
                },
                baseModule,
                module {
                    single { ForcesDefinition(forces, get<ConfigRepository>().config.value) }
                },
                dataModule(),
                shadersModule(),
                sceneModule(),
            )
        }.koin
        val configRepo = application.get<ConfigRepository>()
        val settings = application.get<AppSettings>()
        if (configRepo.currentFile.value == null) {
            val lastOpened = settings.recentProjectPaths.value.firstOrNull()
                ?.let { ConfigPath(it).readContents() }

            if (lastOpened != null) configRepo.openFile(lastOpened)
        }
        configRepo.isDirty = true
        val ui = application.get<AppUI>().ui
        val scene = application.get<ParticlesScene>().scene

        scene.onRelease { sceneScope.cancel() }
        ctx.scenes.stageAdd(scene, index = 0)
        ctx.scenes += ui
        loadedScenes = listOf(scene, ui)
    }

    fun unload() {
        loadedScenes.forEach {
            ctx.removeScene(it)
            it.release()
        }
        loadedScenes = listOf()
    }

    suspend fun open(file: FilePickerResult) {
        val config = globalApplication.koin.get<ConfigRepository>()
        config.openFile(file)
        reload()
    }
}
