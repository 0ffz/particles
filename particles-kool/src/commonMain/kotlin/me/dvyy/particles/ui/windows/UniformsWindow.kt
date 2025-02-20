package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.SimulationButtons
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.liveSlider
import me.dvyy.particles.ui.helpers.sectionTitleStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class UniformsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val uniforms: UniformParameters,
) : FieldsWindow("Live Parameters", ui) {
    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.size(Grow.Std, Grow.Std)
        Column(Grow.Std, Grow.Std) {
            uniforms.uniformParams
                .groupBy { it.first.parameter.path.substringBeforeLast(".").substringAfter(".") }
                .forEach { (name, params) ->
                    Text(name) { sectionTitleStyle() }
                    params.forEach { (param, state) ->
                        liveSlider(
                            param.name,
                            state,
                            min = param.range.start.toFloat(),
                            max = param.range.endInclusive.toFloat()
                        )
                    }
                }
            SimulationButtons()
        }
    }
}
