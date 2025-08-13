package me.dvyy.particles.ui.windows

import com.charleskorn.kaml.YamlNode
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Int32Buffer
import de.fabmax.kool.util.launchOnMainThread
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.deprecated.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import me.dvyy.particles.SceneManager
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.data.VelocitiesDataShader
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.YamlHelpers
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.helpers.FileSystemUtils
import me.dvyy.particles.helpers.initFloat4
import me.dvyy.particles.ui.graphing.GraphState
import me.dvyy.particles.ui.graphing.GraphStyle
import kotlin.time.Duration.Companion.seconds

sealed interface ConfigUiState {
    data class Decoded(val config: ParticlesConfig) : ConfigUiState
    data object Loading : ConfigUiState
    data class Error(val throwable: Throwable) : ConfigUiState
}

class ParticlesViewModel(
    private val buffers: ParticleBuffers,
    private val configRepo: ConfigRepository,
    private val mutableStateScope: CoroutineScope,
    val settings: AppSettings,
    private val sceneManager: SceneManager,
    private val paramOverrides: ParameterOverrides,
    private val scope: CoroutineScope,
    private val velocitiesData: VelocitiesDataShader,
) {
    val plotTexture = Texture2d(
        mipMapping = MipMapping.Off,
        samplerSettings = SamplerSettings().nearest(),
        name = "plot"
    )

    val velocitiesHistogram = GraphState().apply {
        style = GraphStyle.Bar(width = 5.0)
    }

    val configUiState: Flow<ConfigUiState> = configRepo.fileUpdates
        .debounce(1.seconds)
        .mapLatest<PlatformFile, ConfigUiState> {
            val config = it.readString()
            ConfigUiState.Decoded(YamlHelpers.yaml.decodeFromString(ParticlesConfig.serializer(), config))
        }
        .retryWhen { cause, attempt ->
            emit(ConfigUiState.Error(cause))
            true
        }
        .stateIn(scope, SharingStarted.Lazily, ConfigUiState.Loading)

    private val windowFocus = MutableStateFlow(true)

    init {
        // Track window focus as state
        KoolSystem.requireContext().onWindowFocusChanged.stageAdd({ new ->
            windowFocus.update { new.isWindowFocused }
        })

        // Listen to config updates, only when window focus becomes true
        scope.launch {
            combine(configUiState, windowFocus) { configUiState, windowFocus ->
                configUiState to windowFocus
            }
                .filter { it.second }
                .distinctUntilChanged()
                .collect {
                    if(it.first is ConfigUiState.Decoded) reload()
                }
        }
    }

    suspend fun updateVelocityHistogram() {
        val buckets = Int32Buffer(velocitiesData.numBuckets)
        velocitiesData.buckets.downloadData(buckets)
        val bucketsArray = buckets.toArray()
        velocitiesHistogram.render(
            FloatArray(bucketsArray.size) { it.toFloat() },
            bucketsArray.map { it.toFloat() }.toFloatArray()
        )
    }

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
        buffers.positionBuffer.initFloat4 {
            Buffers.randomPosition(configRepo.boxSize)
        }
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
        settings.recentProjectPaths.update { listOf(path) + (it - path) }
        sceneManager.open(FileSystemUtils.toFileOrNull(Path(path)) ?: return@launchOnMainThread)
    }

    fun reload() = launchOnMainThread {
        sceneManager.reloadConfig()
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
