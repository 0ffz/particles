package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.toString
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.SimulationButtons
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuRow
import me.dvyy.particles.ui.helpers.MenuSlider
import me.dvyy.particles.ui.helpers.MenuSlider2
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.helpers.liveSlider
import me.dvyy.particles.ui.helpers.sectionTitleStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class UniformsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val configRepo: ConfigRepository,
    val uniforms: UniformParameters,
    val scope: CoroutineScope,
) : FieldsWindow("Live Parameters", ui) {
    val simsPs = mutableStateOf(0.0)
    val sizeList = listOf(Sizes.small, Sizes.medium, Sizes.large)
    val paramsState = uniforms.uniformParams.asMutableState(scope, default = emptyList())

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
            MenuRow {
                Text("UI size") {
                    labelStyle()
                    modifier.width(Grow.Std)
                }
                ComboBox {
                    modifier.items(listOf("Small", "Medium", "Large"))
                        .selectedIndex(sizeList.indexOf(ui.uiSizes.use()))
                        .onItemSelected { ui.uiSizes.set(sizeList[it]) }
                }
            }
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
