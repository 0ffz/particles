package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.LocalSizes
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Popup
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Button
import de.fabmax.kool.modules.compose.composables.toolkit.Slider
import de.fabmax.kool.modules.compose.composables.toolkit.TextField
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.toString
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.Subcategory
import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun LiveParametersWindow(
    configViewModel: ConfigViewModel = koinInject(),
) {
    Category("Simulation") {
        val simulation by configViewModel.simulation.collectAsState()
        Text(simulation.dT.toString())
        MenuNumber("dT", simulation.dT.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(dT = it.toDouble()) }
        })
        MenuNumber("Max Velocity", simulation.maxVelocity.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(maxVelocity = it.toDouble()) }
        })
        MenuNumber("Max Force", simulation.maxForce.toFloat(), onValueChange = {
            configViewModel.updateSimulation { copy(maxForce = it.toDouble()) }
        })

        ResetSubcategory()
    }
}

@Composable
fun MenuItem(
    name: String,
    content: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(name, Modifier.fillMaxWidth().alignY(AlignmentY.Center))
        content()
    }
}

@Composable
fun MenuNumber(
    name: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    MenuItem(name) {
        TextInputWithTooltip(value, onValueChange)
    }
}

private fun calculateMax(parameter: Float): Float {
    val log = log10(parameter).coerceAtLeast(-3f)
    val power = if (abs(log - ceil(log)) < 0.01f)
        log + 1
    else log
    return 10f.pow(ceil(power))
}

@Composable
fun TextInputWithTooltip(
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    val precision: Int = when (value) {
        in 0f..1f -> 3
        in 0f..100f -> 2
        in 0f..10000f -> 1
        else -> 0
    }
    var valueText by remember(value) { mutableStateOf(value.toString(precision)) }
    remember { mutableStateOf(false) }

    var rangeMax by remember { mutableStateOf(calculateMax(value)) }
    // Column makes popup show up below the text field.
    Column {
        TextField(
            valueText,
            onValueChange = { valueText = it },
            onSubmit = { it.toFloatOrNull()?.let { onValueChange(it) } },
            onFocusChange = { if (it) shown = true },
            modifier = Modifier.fillMaxWidth()
        )
        val colors = LocalColors.current
        if (shown) Popup(relativeToParent = true) {
            Box(
                Modifier.padding(4.dp)
                    .background(RoundRectBackground(colors.background, 6.dp))
            ) {
                Slider(value, onValueChange = onValueChange, 0f..rangeMax, Modifier.width(100.dp))
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
//        }
        }
    }
}

@Composable
private fun ResetSubcategory(
    viewModel: ParticlesViewModel = koinInject(),
    paramsChanged: Boolean = false,
) = Subcategory("Reset") {
    val sizes = LocalSizes.current
    Row(Modifier.fillMaxWidth().padding(sizes.smallGap)) {
        Button(onClick = { viewModel.resetPositions() }, Modifier.fillMaxWidth()) {
            Text("Positions")
        }
        Box(Modifier.width(sizes.smallGap)) { }
        Button(onClick = { viewModel.resetParameters() }, Modifier.fillMaxWidth()) {
            Text(if (paramsChanged) "(*) Parameters" else "Parameters")
        }
    }
}
