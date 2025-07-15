package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.SimulationButtons
import me.dvyy.particles.ui.components.IconButton
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuSlider2
import me.dvyy.particles.ui.helpers.sectionSubtitleStyle
import me.dvyy.particles.ui.helpers.sectionTitleStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class UniformsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val configRepo: ConfigRepository,
    val uniforms: UniformParameters,
    val scope: CoroutineScope,
    val paramOverrides: ParameterOverrides,
) : FieldsWindow(
    name = "Live Parameters",
    ui = ui,
    icon = Icons.slidersHorizontal,
    preferredWidth = 300f,
) {
    val paramsState = uniforms.uniformParams.asMutableState(scope)
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
                paramsState
                    .use()
                    .groupBy { it.configPath.substringBeforeLast(".").substringAfter(".") }
                    .forEach { (name, params) ->
                        Subcategory(name) {
                            params.forEach { param ->
                                val isOverridden = overrides.use().contains(param.configPath)
                                ParameterSlider(param, isOverridden, onChange = {
                                    viewModel.updateOverrides(param.configPath, it, Float.serializer())
                                })
                            }
                        }
                    }
            }
        }
    }
}

//private fun UiScope.ParameterGraph() {
//    val graph = remember {
//        LineGraphNode().apply {
//            launchOnMainThread { renderGpuFunction(scene, TestForce) }
//        }
//    }
//    Box(Grow.Std, 400.dp) {
//        modifier.background(graph)
//    }
//}

private fun UiScope.ParameterSlider(
    param: UniformParameter<Float>,
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
