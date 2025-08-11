package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenu
import de.fabmax.kool.modules.compose.composables.toolkit.Slider
import de.fabmax.kool.modules.compose.composables.toolkit.TextField
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import de.fabmax.kool.modules.compose.modifiers.onClick
import de.fabmax.kool.modules.compose.modifiers.width
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.toString
import kotlin.math.*

@Composable
fun TextInputWithTooltip(
    value: Number,
    onValueChange: (Number) -> Unit,
) = Column { // Column makes popup show up below the text field.
    var shown by remember { mutableStateOf(false) }
    val precision = calculatePrecision(value)
    var valueText by remember(value) { mutableStateOf(value.toDouble().toString(precision)) }
    var rangeMax by remember { mutableStateOf(calculateMax(value)) }

    fun Double.roundToPrecision() = (this * 10.0.pow(precision)).roundToInt().toDouble() / 10.0.pow(precision)
    TextField(
        valueText,
        onValueChange = { valueText = it },
        onSubmit = {
            val number = it.toDoubleOrNull()
            if (number != null) onValueChange(number.roundToPrecision())
            else onValueChange(value) // reset on invalid input
        },
//        onFocusChange = { if (it) shown = true },
        modifier = Modifier.fillMaxWidth().onClick { shown = true }
    )

    DropdownMenu(shown, onDismissRequest = { shown = false }) {
        Slider(value.toFloat(), onValueChange = { onValueChange(it.toDouble().roundToPrecision()) }, 0f..rangeMax, Modifier.width(100.dp))
    }
//        LaunchedEffect(Unit) {
//            withContext(Dispatchers.RenderLoop) {
//                while(true) {
//                    val ptr = PointerInput.primaryPointer
//                    if (ptr.isAnyButtonDown && !clickedInBounds.value) {
//                        if (uiNode.isInBounds(ptr.pos))
//                            clickedInBounds.set(true)
//                        else shown.set(false)
//                    }
//                    if (ptr.isAnyButtonReleased && !uiNode.isInBounds(ptr.pos) && !clickedInBounds.value) {
//                        shown.set(false)
//                    }
//                    if (ptr.isAnyButtonReleased) {
//                        maxRange.set(calculateMax())
//                        clickedInBounds.set(false)
//                    }
//                    yield()
//                }
//            }
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
