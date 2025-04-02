package me.dvyy.particles

import com.russhwolf.settings.Settings
import de.fabmax.kool.KoolContext
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.delayFrames
import de.fabmax.kool.util.launchOnMainThread
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.io.files.Path
import me.dvyy.particles.compute.ForcesDefinition
import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.FileSystemUtils
import me.dvyy.particles.ui.AppUI
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SceneManager(
    val ctx: KoolContext,
    /** Classes/data that persists across application reloads. */
    val baseModule: Module,
    val forces: List<Force>
) {
    private var loadedScenes: List<Scene> = listOf()
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
        if(configRepo.currentFile.value == null) {
            settings.recentProjectPaths.value.firstOrNull()
                ?.let { FileSystemUtils.toFileOrNull(Path(it)) }
                ?.let { configRepo.openFile(it) }
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

    suspend fun open(file: PlatformFile) {
        val config = globalApplication.koin.get<ConfigRepository>()
        config.openFile(file)
        reload()
    }
}
