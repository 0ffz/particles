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
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuSlider2
import me.dvyy.particles.ui.helpers.sectionTitleStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class UniformsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val configRepo: ConfigRepository,
    val uniforms: UniformParameters,
    val scope: CoroutineScope,
) : FieldsWindow("Live Parameters", ui, Icons.slidersHorizontal) {
    val paramsState = uniforms.uniformParams.asMutableState(scope, default = emptyList())

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std, Grow.Std) {
            Text("Simulation") { sectionTitleStyle() }
            val state = viewModel.uiState.use()
            state.forEach {
                with(it) { draw() }
            }
            paramsState
                .use()
                .groupBy { it.configPath.substringBeforeLast(".").substringAfter(".") }
                .forEach { (name, params) ->
                    Text(name) { sectionTitleStyle() }
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
            SimulationButtons(viewModel)
        }
    }
}
