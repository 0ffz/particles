package me.dvyy.particles.ui.windows

import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.components.IconButton
import me.dvyy.particles.ui.helpers.*
import me.dvyy.particles.ui.nodes.GraphState
import me.dvyy.particles.ui.viewmodels.ForceParametersViewModel
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

class UniformsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val configRepo: ConfigRepository,
    val scope: CoroutineScope,
    val forceParametersViewModel: ForceParametersViewModel,
    val paramOverrides: ParameterOverrides,
) : FieldsWindow(
    name = "Live Parameters",
    ui = ui,
    icon = Icons.slidersHorizontal,
    preferredWidth = 300f,
) {
    //    val paramsState = uniforms.uniformParams.asMutableState(scope)
    val forceStates = forceParametersViewModel.parameters.asMutableState(scope, arrayOf())
    val overrides = paramOverrides.overrides.asMutableState(scope, mapOf())

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        vScrollbarModifier = { it },
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std).padding(end = 8.dp)
        Column(Grow.Std, Grow.Std) {
            Category("Simulation") {
                val state = viewModel.uiState.use()
                Column(Grow.Std) {
                    state.forEach {
                        with(it) { draw() }
                    }
                }
                TODO("Remove")
            }
            Category("Interactions") {
                forceStates.use().forEach { force ->
                    Subcategory(force.name) {
//                        ParameterGraph(forceParametersViewModel.graph)
                        Row(Grow.Std) {
                            Column(width = Grow.Std) {
                                Text("Pair") { }
                                force.interactions.forEachIndexed { row, interaction ->
                                    Box(width = Grow.Std) {
                                        val bg = if (row % 2 == 0) Color.WHITE.withAlpha(0.1f) else Color.TRANSPARENT
                                        modifier.backgroundColor(bg)
                                        Row {
                                            interaction.set.ids.forEachIndexed { i, it ->
                                                val particle = configRepo.config.value.particles[it.id]
                                                val name = configRepo.config.value.particleName(it)
                                                Text(name) {
                                                    labelStyle()
                                                    modifier.textColor(Color(particle.color).mix(Color.WHITE, 0.7f))
                                                }
                                                if (i != interaction.set.ids.lastIndex)
                                                    Text("-") { labelStyle(); modifier.textColor(Color.LIGHT_GRAY) }
                                            }
                                        }
                                    }
                                }
                            }
                            force.interactions.firstOrNull()?.parameters?.indices?.forEach { i ->
                                Column {
                                    Text(force.interactions.first().parameters[i].name) { modifier.padding(horizontal = 8.dp) }
                                    force.interactions.forEachIndexed { row, interaction ->
                                        Box(Grow.MinFit) {
                                            val bg =
                                                if (row % 2 == 0) Color.WHITE.withAlpha(0.1f) else Color.TRANSPARENT
                                            modifier.padding(horizontal = 8.dp).backgroundColor(bg)
                                            val parameter = interaction.parameters[i]
                                            TextInputWithTooltip(parameter, { new ->
                                                forceParametersViewModel.updateParameter(
                                                    force = force.name,
                                                    interaction = interaction.set,
                                                    name = parameter.name,
                                                    value = new
                                                )
                                            }, width = Grow.MinFit)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
//                paramsState
//                    .use()
//                    .groupBy { it.configPath.substringBeforeLast(".").substringAfter(".") }
//                    .forEach { (name, params) ->
//                        Subcategory(name) {
//                            params.forEach { param ->
//                                val isOverridden = overrides.use().contains(param.configPath)
//                                ParameterSlider(param, isOverridden, onChange = {
//                                    viewModel.updateOverrides(param.configPath, it, Float.serializer())
//                                })
//                            }
//                        }
//                    }
            }
        }
    }
}


fun UiScope.TextInputWithTooltip(
    parameter: UniformParameter,
    onChange: (Float) -> Unit,
    width: Dimension = FitContent
) {
    ParameterTextInput(parameter, onChange = { onChange(it) }) {
        val position = remember(Vec2f.ZERO)
        val height = remember(20f)
        val shown = remember(false)
        modifier
            .width(width)
            .onPositioned { if (!shown.value) position.set(Vec2f(it.leftPx, it.topPx)) }
            .onMeasured { height.set(it.heightPx) }
        if (isFocused.use()) shown.set(true)
        if (shown.use()) {
            val position = position.use()
            val height = height.use()
            Popup(
                position.x.coerceAtMost(uiNode.surface.viewport.widthPx - 200.dp.px).coerceAtLeast(0f),
                if (position.y + 2 * height < uiNode.surface.viewport.heightPx)
                    position.y + height
                else position.y - height,
                width = 200.dp,
                height = FitContent
            ) {
                modifier.padding(4.dp).background(
                    RoundRectBackground(
                        surface.colors.background,
                        6.dp
                    )
                )
                fun calculateMax(): Float {
                    val log = log10(parameter.value).coerceAtLeast(-3f)
                    val power = if (abs(log - ceil(log)) < 0.01f)
                        log + 1
                    else log
                    return 10f.pow(ceil(power))
                }

                val maxRange = remember { mutableStateOf(calculateMax()) }
                MenuSlider2(
                    "",
                    parameter.value,
                    min = parameter.range.start.toFloat(),
                    max = maxRange.use(),
                    precision = parameter.precision,
                    onChange = { onChange(it) },
                ) {
                    val clickedInBounds = remember(false)
                    // Logic for hiding the slider once we've clicked off of it
                    surface.onEachFrame {
                        val ptr = PointerInput.primaryPointer
                        if (ptr.isAnyButtonDown && !clickedInBounds.value) {
                            if (uiNode.isInBounds(ptr.pos))
                                clickedInBounds.set(true)
                            else shown.set(false)
                        }
                        if (ptr.isAnyButtonReleased && !uiNode.isInBounds(ptr.pos) && !clickedInBounds.value) {
                            shown.set(false)
                        }
                        if (ptr.isAnyButtonReleased) {
                            maxRange.set(calculateMax())
                            clickedInBounds.set(false)
                        }
                    }
                }
            }
        }
    }
}

fun UiScope.ParameterGraph(graph: GraphState) {
    Box(Grow.Std, 400.dp) {
        modifier.background(graph)
    }
}

fun UiScope.ParameterTextInput(
    param: UniformParameter,
    onChange: (Float) -> Unit,
    block: TextFieldScope.() -> Unit = {},
) {
    MenuTextInput(
        value = param.value,
        min = param.range.start.toFloat(),
        max = param.range.endInclusive.toFloat(),
        precision = param.precision,
        onChange = onChange,
        block = block,
    )
}

fun UiScope.Category(name: String, desc: String? = null, content: UiScope.() -> Unit) {
    TODO("Remove")
}

fun UiScope.Subcategory(name: String, content: UiScope.() -> Unit) {
    TODO("Remove")
}

fun UiScope.ToggleButton(
    toggled: Boolean,
    small: Boolean = false,
    onToggle: (Boolean) -> Unit,
) {
    val size = if (small) 24.dp else 40.dp
    val icon = if (toggled) Icons.chevronUp else Icons.chevronDown
    IconButton(icon, onClick = { onToggle(!toggled) }) {
        modifier.size(size, size)
    }
}
