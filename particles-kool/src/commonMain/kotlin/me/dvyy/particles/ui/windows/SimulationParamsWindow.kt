package me.dvyy.particles.ui.windows

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.toString
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class SimulationParamsWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
    private val configRepo: ConfigRepository,
) : FieldsWindow("Simulation parameters", ui) {
    val simsPs = mutableStateOf(0.0)
    override fun Scene.setup(ctx: KoolContext) {
        onUpdate {
            simsPs.set(ctx.fps * configRepo.config.value.simulation.passesPerFrame)
        }
    }

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.size(Grow.Std, Grow.Std)
        Column(Grow.Std, Grow.Std) {
            Text("${simsPs.use().toString(2)} sims/s") {}
            val state = viewModel.uiState.use()
            state.forEach {
                with(it) { draw() }
            }
        }
    }
}
