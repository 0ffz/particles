package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.toString
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class SimulationStatisticsWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
    private val configRepo: ConfigRepository,
) : FieldsWindow("Simulation Stats", ui) {
    val simsPs = mutableStateOf(0.0)
    val fps = mutableStateOf(0.0)

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        withVerticalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        surface.onEachFrame {
            fps.set(it.fps)
            simsPs.set(it.fps * configRepo.config.value.simulation.passesPerFrame)
        }
        Column(Grow.Std, Grow.Std) {
            Text("${simsPs.use().toString(2)} sims/s") {}
//            Text("${fps.use().toString(2)} fps") {}
        }
    }
}
