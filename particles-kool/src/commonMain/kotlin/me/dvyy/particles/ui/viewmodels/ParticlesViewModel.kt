package me.dvyy.particles.ui.viewmodels

import com.charleskorn.kaml.YamlNode
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import me.dvyy.particles.SceneManager
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.YamlHelpers
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.helpers.initFloat3
import me.dvyy.particles.helpers.initFloat4
import me.dvyy.particles.ui.helpers.UiConfigurable
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class ParticlesViewModel(
    private val buffers: ParticleBuffers,
    private val configRepo: ConfigRepository,
    private val mutableStateScope: CoroutineScope,
    private val sceneManager: SceneManager,
) {
    val uiState: MutableStateValue<List<UiConfigurable>> = configRepo.config.map { it.simulation }
        .distinctUntilChanged()
        .map { state ->
            listOf(
//                UiConfigurable.Slider("Count", state.targetCount, 0f, 100_000f, precision = 0) {
//                    updateState { copy(targetCount = it.roundToInt()) }
//                },
//                UiConfigurable.Slider("Min Grid Size", state.minGridSize, 0f, 100f) {
//                    updateState { copy(minGridSize = it.toDouble()) }
//                },
                UiConfigurable.Slider("dT", state.dT, 0f, 0.01f, precision = 4) {
                    updateState { copy(dT = it.toDouble()) }
                },
                UiConfigurable.Slider("Max Velocity", state.maxVelocity, 0f, 100f) {
                    updateState { copy(maxVelocity = it.toDouble()) }
                },
                UiConfigurable.Slider("Max Force", state.maxForce, 0f, 100_000f) {
                    updateState { copy(maxForce = it.toDouble()) }
                },
//                UiConfigurable.Toggle("3d", state.threeDimensions) {
//                    updateState { copy(threeDimensions = it) }
//                },
            )
        }
        .asMutableState(mutableStateScope, default = listOf())

    init {
        launchOnMainThread {
            configRepo.parameters.overrides.debounce(1.seconds).collect {
                saveParameters()
            }
        }
    }

    fun updateState(simulation: Simulation.() -> Simulation) = launchOnMainThread {
        val config = configRepo.config.value
        val newSimulation = simulation(config.simulation)
        configRepo.updateConfig(config.copy(simulation = newSimulation))
    }

    fun <T> updateOverrides(key: String, newValue: T, serializer: KSerializer<T>) = launchOnMainThread {
        configRepo.parameters.update(
            key, YamlHelpers.yaml.decodeFromString(
                YamlNode.serializer(),
                YamlHelpers.yaml.encodeToString(serializer, newValue)
            )
        )
    }

    fun resetPositions() = launchOnMainThread {
        buffers.positionBuffer.initFloat4 {
            Buffers.randomPosition(configRepo.boxSize)
        }
        buffers.initializeParticlesBuffer()
    }

    fun restartSimulation() = launchOnMainThread {
        sceneManager.reload()
    }

    fun saveParameters() = launchOnMainThread {
        configRepo.saveParameters()
    }

    fun resetParameters() = launchOnMainThread {
        configRepo.resetParameters()
    }
}
