package me.dvyy.particles.ui.helpers

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.UiScope
import kotlin.jvm.JvmName

fun UiScope.liveSlider(
    name: String, state: MutableStateValue<Float>,
    min: Float = 0f,
    max: Float = 1f,
    precision: Int = 2,
): Unit = MenuSlider2(
    name,
    state.use(),
    min = min,
    max = max,
    precision = precision,
    onChange = { state.value = it },
)

@JvmName("liveSliderInt")
fun UiScope.liveSlider(
    name: String, state: MutableStateValue<Int>,
    min: Float = 0f,
    max: Float = 1f,
): Unit = MenuSlider2(
    name,
    state.use().toFloat(),
    min = min,
    max = max,
    txtFormat = { it.toInt().toString() },
    onChange = { state.value = it.toInt() },
)
