package me.dvyy.particles.ui

import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.YamlParameters
import me.dvyy.particles.asMutableState

class FieldsState(
    val params: YamlParameters,
    val scope: CoroutineScope,
) {
    val count = params.get<Int>("simulation.count", default = 100)//.asMutableState(scope, default = 100)
    val epsilon = params.get<Float>("simulation.epsilon", default = 100f)//.asMutableState(scope, default = 100f)
//    val fileText = mutableStateOf("")
}
