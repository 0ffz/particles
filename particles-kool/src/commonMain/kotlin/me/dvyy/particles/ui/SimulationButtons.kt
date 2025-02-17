package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.*
import me.dvyy.particles.ui.helpers.inject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

fun UiScope.SimulationButtons() {
    val viewModel = inject<ParticlesViewModel>()
    Column(Grow.Std, Grow.Std) {
        modifier.padding(sizes.smallGap)
        Button("Reset positions") {
            modifier.onClick { viewModel.resetPositions() }.margin(bottom = sizes.smallGap)
        }
        Button("Restart simulation") {
            modifier.onClick { viewModel.restartSimulation() }.margin(bottom = sizes.smallGap)
        }
        Row(Grow.Std) {
            Button("Save") {
                modifier.onClick { viewModel.save() }.margin(end = sizes.smallGap)
            }
            Button("Load") {
                modifier.onClick { viewModel.load() }
            }
        }
    }
}
