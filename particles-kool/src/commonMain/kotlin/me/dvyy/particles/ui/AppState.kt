package me.dvyy.particles.ui

import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.YamlParameters

class AppState(
    val params: YamlParameters,
    val scope: CoroutineScope,
) {
    val targetCount = params.get<Int>("simulation.count", default = 10_000)
    val minGridSize = params.get<Float>("simulation.minGridSize", default = 5f)
    val dT = params.get<Float>("simulation.dT", default = 0.001f)
    val maxVelocity = params.get<Float>("simulation.maxVelocity", default = 100f)
    val maxForce = params.get<Float>("simulation.maxForce", default = 10_000f)
    val threeDimensions = params.get<Boolean>("simulation.threeDimensions", default = false)
    val passesPerFrame = params.get<Int>("simulation.passesPerFrame", default = 10)

    val width = params.get<Int>("simulation.size.width", default = 2000)
    val height = params.get<Int>("simulation.size.height", default = 1400)
    val depth = params.get<Int>("simulation.size.depth", default = 2000)
//    val fileText = mutableStateOf("")
}
