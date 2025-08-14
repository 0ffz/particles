package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.*
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenu
import de.fabmax.kool.modules.compose.composables.toolkit.Slider
import de.fabmax.kool.modules.compose.composables.toolkit.TextField
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.toString
import kotlin.math.*

@Composable
fun TextInputWithTooltip(
    value: Number,
    modifier: Modifier = Modifier,
    onValueChange: (Number) -> Unit,
) = Column { // Column makes popup show up below the text field.
    var shown by remember { mutableStateOf(false) }
    val precision = calculatePrecision(value)
    var valueText by remember(value) { mutableStateOf(value.toDouble().toString(precision)) }
    var rangeMax by remember { mutableStateOf(calculateMax(value)) }

    val round = 10.0.pow(precision)
    fun boundedChange(value: Double) {
        onValueChange(((value.coerceAtLeast(0.0) * round).roundToInt() / round))
    }

    TextField(
        valueText,
        onValueChange = { valueText = it },
        onSubmit = {
            val number = it.toDoubleOrNull()
            if (number != null) boundedChange(number)
            else onValueChange(value) // reset on invalid input
        },
        modifier = Modifier.fillMaxWidth().onClick { shown = true }
            .onWheelY {
                // Don't round on wheel y for smooth scrolling
                val multiplier = if (KeyboardInput.isShiftDown) 1.0 else 10.0
                onValueChange(
                    (value.toDouble() + multiplier * (0.1f).pow(precision) * it.pointer.scroll.y)
                        .coerceAtLeast(0.0)
                )
            }
            .then(modifier)
    )

    DropdownMenu(shown, onDismissRequest = { shown = false }) {
        Slider(value.toFloat(), onValueChange = { boundedChange(it.toDouble()) }, 0f..rangeMax, Modifier.width(100.dp))
    }
}

private fun calculatePrecision(value: Number): Int {
    if (value is Int) return 0
    return when (value.toFloat()) {
        in 0f..1f -> 3
        in 0f..100f -> 2
        in 0f..10000f -> 1
        else -> 0
    }
}

private fun calculateMax(parameter: Number): Float {
    val log = log10(parameter.toFloat()).coerceAtLeast(-3f)
    val power = if (abs(log - ceil(log)) < 0.01f)
        log + 1
    else log
    return 10f.pow(ceil(power))
}
