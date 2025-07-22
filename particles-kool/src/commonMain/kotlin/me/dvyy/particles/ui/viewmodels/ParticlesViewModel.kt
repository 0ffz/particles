package me.dvyy.particles.ui.viewmodels

import com.charleskorn.kaml.YamlNode
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.launchOnMainThread
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.deprecated.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import me.dvyy.particles.SceneManager
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.YamlHelpers
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.helpers.FileSystemUtils
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.helpers.UiConfigurable

class ParticlesViewModel(
    private val buffers: ParticleBuffers,
    private val configRepo: ConfigRepository,
    private val mutableStateScope: CoroutineScope,
    val settings: AppSettings,
    private val sceneManager: SceneManager,
    private val paramOverrides: ParameterOverrides,
    private val scope: CoroutineScope,
) {
    val passesPerFrame = MutableStateFlow(1)
    val uiState: MutableStateValue<List<UiConfigurable>> = configRepo.config.map { it.simulation }
        .distinctUntilChanged()
        .map { state ->
            listOf(
                UiConfigurable.Slider("dT", state.dT, 0f, 0.01f, precision = 4) {
                    updateState { copy(dT = it.toDouble()) }
                },
                UiConfigurable.Slider("Max Velocity", state.maxVelocity, 0f, 100f) {
                    updateState { copy(maxVelocity = it.toDouble()) }
                },
                UiConfigurable.Slider("Max Force", state.maxForce, 0f, 100_000f) {
                    updateState { copy(maxForce = it.toDouble()) }
                },
            )
        }
        .asMutableState(mutableStateScope, default = listOf())

    val plotTexture = Texture2d(
        mipMapping = MipMapping.Off,
        samplerSettings = SamplerSettings().nearest(),
        name = "plot"
    )

    fun updateState(simulation: Simulation.() -> Simulation) = scope.launch {
        val config = configRepo.config.value
        val newSimulation = simulation(config.simulation)
        configRepo.updateConfig(config.copy(simulation = newSimulation))
    }

    fun <T> updateOverrides(key: String, newValue: T, serializer: KSerializer<T>) = scope.launch {
        paramOverrides.update(
            key, YamlHelpers.yaml.decodeFromString(
                YamlNode.serializer(),
                YamlHelpers.yaml.encodeToString(serializer, newValue)
            )
        )
    }

    fun resetPositions() = scope.launch {
        buffers.initializeParticlesBuffer()
    }

    fun restartSimulation() = launchOnMainThread { // scope will get cancelled during reload, so we use main thread
        sceneManager.reload()
    }

    fun resetParameters() = scope.launch {
        paramOverrides.reset()
    }

    fun attemptOpenProject() = launchOnMainThread {
        val file = FileKit.openFilePicker(type = FileKitType.File("yml")) ?: return@launchOnMainThread
        sceneManager.open(file)
        val path = FileSystemUtils.getPathOrNull(file) ?: return@launchOnMainThread
        settings.recentProjectPaths.update { it + path.toString() }
    }

    fun openProject(path: String) = launchOnMainThread {
        sceneManager.open(FileSystemUtils.toFileOrNull(Path(path)) ?: return@launchOnMainThread)
        settings.recentProjectPaths.update { listOf(path) + (it - path) }
    }

    fun removeProject(path: String) = launchOnMainThread {
        settings.recentProjectPaths.update { it - path }
    }

    fun saveClusterData() {
        launchOnMainThread {
            val info = buffers.clusterInfo?.sizes ?: return@launchOnMainThread
            FileKit.openFileSaver(info.joinToString("\n").encodeToByteArray(), "data", "csv")
        }
    }
}
