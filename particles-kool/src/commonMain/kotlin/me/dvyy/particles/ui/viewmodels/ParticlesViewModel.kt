package me.dvyy.particles.ui.viewmodels

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.dvyy.particles.SceneManager
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.asMutableState
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.ui.helpers.UiConfigurable
import kotlin.math.roundToInt
import kotlin.random.Random

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
                UiConfigurable.Slider("dT", state.dT, 0f, 0.05f, precision = 3) {
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

    fun updateState(simulation: Simulation.() -> Simulation) = launchOnMainThread {
        val config = configRepo.config.value
        val newSimulation = simulation(config.simulation)
        configRepo.updateConfig(config.copy(simulation = newSimulation))
    }

    fun resetPositions() = launchOnMainThread {
        val positions = buffers.positionBuffer
        val simulation = configRepo.config.value.simulation
        for (i in 0 until configRepo.count) {
            positions[i] = Vec4f(
                Random.Default.nextInt(simulation.size.width).toFloat(),
                Random.Default.nextInt(simulation.size.height).toFloat(),
                if (!simulation.threeDimensions || simulation.size.depth == 0) 0f
                else Random.Default.nextInt(simulation.size.depth).toFloat(),
                0f,
            )
        }
        buffers.initializeParticlesBuffer()
    }

    fun restartSimulation() = launchOnMainThread {
        sceneManager.reload()
    }

    fun save() = launchOnMainThread {
        configRepo.saveConfig()
    }

    fun load() = launchOnMainThread {
        configRepo.loadConfig()
    }
}
