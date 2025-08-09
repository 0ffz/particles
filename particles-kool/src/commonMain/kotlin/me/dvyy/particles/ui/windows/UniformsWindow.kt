package me.dvyy.particles.ui.windows

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuTextInput
import me.dvyy.particles.ui.helpers.TRANSPARENT
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.nodes.GraphState
import me.dvyy.particles.ui.viewmodels.ForceParametersViewModel
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class UniformsWindow(
    val viewModel: ParticlesViewModel,
    val configRepo: ConfigRepository,
    val scope: CoroutineScope,
    val forceParametersViewModel: ForceParametersViewModel,
    val paramOverrides: ParameterOverrides,
) : FieldsWindow() {
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
            Category("Interactions") {
                forceStates.use().forEach { force ->
                    Subcategory(force.name) {
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

