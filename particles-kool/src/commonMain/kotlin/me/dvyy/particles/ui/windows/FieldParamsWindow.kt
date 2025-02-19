package me.dvyy.particles.ui.windows

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.toString
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.ui.AppState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.SimulationButtons
import me.dvyy.particles.ui.UniformParameters
import me.dvyy.particles.ui.helpers.*
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import kotlin.math.roundToInt

class FieldParamsWindow(
    ui: AppUI,
    val viewModel: ParticlesViewModel,
    val uniforms: UniformParameters,
) : FieldsWindow("Live Parameters", ui) {
    val simsPs = mutableStateOf(0.0)
    override fun Scene.setup(ctx: KoolContext) {
        onUpdate {
            simsPs.set(ctx.fps * viewModel.simulation.value.passesPerFrame)
        }
    }

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.size(Grow.Std, Grow.Std)
        Column(Grow.Std, Grow.Std) {
            Text("${simsPs.use().toString(2)} sims/s") {}
            val state = viewModel.simulation.use()
            MenuSlider2("Count", state.count.toFloat(), max = 100_000f, onChange = { viewModel.updateState(state.copy(count = it.roundToInt())) })
//            liveSlider("Min grid size", state.minGridSize, max = 100f)
//            liveSlider("dT", state.dT, max = 0.05f, precision = 3)
//            liveSlider("Max velocity", state.maxVelocity, max = 100f)
//            liveSlider("Max force", state.maxForce, max = 100_000f)
//            LabeledSwitch("3d", state.threeDimensions)

            uniforms.uniformParams
                .groupBy { it.first.parameter.path.substringBeforeLast(".").substringAfter(".") }
                .forEach { (name, params) ->
                    Text(name) { sectionTitleStyle() }
                    params.forEach { (param, state) ->
                        liveSlider(param.name, state, min = param.range.start.toFloat(), max = param.range.endInclusive.toFloat())
                    }
                }
            SimulationButtons()
        }
    }
}
