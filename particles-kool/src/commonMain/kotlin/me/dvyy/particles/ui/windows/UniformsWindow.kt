package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.config.ConfigRepository
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
) : FieldsWindow(
    name = "Live Parameters",
    ui = ui,
    icon = Icons.slidersHorizontal,
    preferredWidth = 300f,
) {
    val paramsState = uniforms.uniformParams.asMutableState(scope)

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
                SimulationButtons(viewModel)
            }
            Category("Interactions") {
                paramsState
                    .use()
                    .groupBy { it.configPath.substringBeforeLast(".").substringAfter(".") }
                    .forEach { (name, params) ->
                        Subcategory(name) {
                            params.forEach { param ->
                                MenuSlider2(
                                    param.name,
                                    param.value,
                                    min = param.range.start.toFloat(),
                                    max = param.range.endInclusive.toFloat(),
                                    onChange = { viewModel.updateOverrides(param.configPath, it, Float.serializer()) },
                                )
                            }
                        }
                    }
            }
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
        val icon = if (toggled.use()) Icons.chevronUp else Icons.chevronDown
        IconButton(icon, onClick = { toggled.set(!toggled.value) }) {
            modifier.size(40.dp, 40.dp)
        }
    }
    if (toggled.use()) {
        if (desc != null) Text("*$desc") {
            modifier.padding(sizes.smallGap).textColor(colors.onBackgroundAlpha(0.5f)).isWrapText(true).width(Grow.Std)
        }
        content()
    }
}

fun UiScope.Subcategory(name: String, content: UiScope.() -> Unit) {
    Text(name) {
        modifier.backgroundColor(colors.primaryVariant.withAlpha(0.1f))
        sectionSubtitleStyle()
    }
    content()
}
