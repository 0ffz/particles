package me.dvyy.particles.ui

import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.YamlParameters
import me.dvyy.particles.asMutableState

class FieldsState(
    val params: YamlParameters,
    val scope: CoroutineScope,
) {
    val targetCount = params.get<Int>("simulation.count", default = 10_000)//.asMutableState(scope, default = 100)
    val minGridSize = params.get<Float>("simulation.minGridSize", default = 5f)
    val epsilon = params.get<Float>("simulation.epsilon", default = 100f)//.asMutableState(scope, default = 100f)
    val threeDimensions = params.get<Boolean>("simulation.threeDimensions", default = false)
    val dT = params.get<Float>("simulation.dT", default = 0.001f)
//    val fileText = mutableStateOf("")
}
