package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.toString
import me.dvyy.particles.config.ConfigRepository
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
    val configRepo: ConfigRepository,
    val uniforms: UniformParameters,
) : FieldsWindow("Live Parameters", ui) {
    val simsPs = mutableStateOf(0.0)
    val sizeList = listOf(Sizes.small, Sizes.medium, Sizes.large)

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        surface.onEachFrame {
            simsPs.set(it.fps * configRepo.config.value.simulation.passesPerFrame)
        }
        Column(Grow.Std, Grow.Std) {
            Text("Simulation") { sectionTitleStyle() }
            Text("${simsPs.use().toString(2)} sims/s") {}
            ComboBox {
                modifier.items(listOf("Small", "Medium", "Large"))
                    .selectedIndex(sizeList.indexOf(ui.uiSizes.use()))
                    .onItemSelected { ui.uiSizes.set(sizeList[it]) }
            }
            val state = viewModel.uiState.use()
            state.forEach {
                with(it) { draw() }
            }
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
            SimulationButtons(viewModel)
        }
    }
}
