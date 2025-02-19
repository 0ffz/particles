package me.dvyy.particles.ui.viewmodels

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.launchOnMainThread
import me.dvyy.particles.FieldsBuffers
import me.dvyy.particles.YamlParameters
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.ui.AppState
import kotlin.random.Random

class ParticlesViewModel(
    private val state: AppState,
    private val buffers: FieldsBuffers,
    private val parameters: YamlParameters,
    private val config: ParticlesConfig,
) {
    val simulation = mutableStateOf(config.simulation)

    fun updateState(simulation: Simulation) {
        this.simulation.set(simulation)
    }

    fun resetPositions() = launchOnMainThread {
        val positions = buffers.positionBuffer
        for (i in 0 until state.count) {
            positions[i] = Vec4f(
                Random.Default.nextInt(state.width.value).toFloat(),
                Random.Default.nextInt(state.height.value).toFloat(),
                if (!state.threeDimensions.value || state.depth.value == 0) 0f
                else Random.Default.nextInt(state.depth.value).toFloat(),
                0f,
            )
        }
    }

    fun restartSimulation() = launchOnMainThread {

    }

    fun save() = launchOnMainThread {
        parameters.save()
    }

    fun load() = launchOnMainThread {
        parameters.load()
    }
}
