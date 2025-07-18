package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.SimulationButtons
import me.dvyy.particles.ui.components.IconButton
import me.dvyy.particles.ui.helpers.*
import me.dvyy.particles.ui.nodes.LineGraphNode
import me.dvyy.particles.ui.viewmodels.ForceParametersViewModel
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

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
                state.forEach {
                    with(it) { draw() }
                }
                SimulationButtons(viewModel, paramsChanged = overrides.use().isNotEmpty())
            }
            Category("Interactions") {
                forceStates.use().forEach { force ->
                    40.dp
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
                                            ParameterTextInput(parameter, onChange = { new ->
                                                forceParametersViewModel.updateParameter(
                                                    force = force.name,
                                                    interaction = interaction.set,
                                                    name = parameter.name,
                                                    value = new
                                                )
                                            })
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

private fun UiScope.ParameterGraph(graph: LineGraphNode) {
    Box(Grow.Std, 400.dp) {
        modifier.background(graph)
    }
}

private fun UiScope.ParameterSlider(
    param: UniformParameter,
    isOverridden: Boolean,
    onChange: (Float) -> Unit,
) {
    val showSlider = remember(false)
    Row(width = Grow.Std) {
        Column(width = Grow.Std) {
            MenuSlider2(
                if (isOverridden) param.name + "(*)" else param.name,
                param.value,
                min = param.range.start.toFloat(),
                max = param.range.endInclusive.toFloat(),
                onChange = { onChange(it) },
                sliderShown = showSlider.use(),
            )
        }
        ToggleButton(showSlider.use(), small = true) {
            showSlider.set(it)
        }
    }
}

private fun UiScope.ParameterTextInput(
    param: UniformParameter,
    onChange: (Float) -> Unit,
) {
    MenuTextInput(
        value = param.value,
        min = param.range.start.toFloat(),
        max = param.range.endInclusive.toFloat(),
        precision = param.precision,
        onChange = onChange,
    )
}

fun UiScope.Category(name: String, desc: String? = null, content: UiScope.() -> Unit) {
    val toggled = remember(true)
    Row(Grow.Std) {
        modifier.backgroundColor(colors.primaryVariant.withAlpha(0.2f))
            .onClick { toggled.set(!toggled.value) }
        Text(name) {
            sectionTitleStyle()
        }
        ToggleButton(toggled.use()) { toggled.set(it) }
    }
    if (toggled.use()) {
        if (desc != null) Text("*$desc") {
            modifier.padding(sizes.smallGap).textColor(colors.onBackgroundAlpha(0.5f)).isWrapText(true).width(Grow.Std)
        }
        content()
    }
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

fun UiScope.Subcategory(name: String, content: UiScope.() -> Unit) {
    Text(name) {
        modifier.backgroundColor(colors.primaryVariant.withAlpha(0.1f))
        sectionSubtitleStyle()
    }
    content()
}
