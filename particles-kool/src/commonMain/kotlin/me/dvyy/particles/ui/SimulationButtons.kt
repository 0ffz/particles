package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.*
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

fun UiScope.SimulationButtons(viewModel: ParticlesViewModel) {
    Column(Grow.Std, Grow.Std) {
        modifier.padding(sizes.smallGap)
        Row(Grow.Std) {
            Button("Reset positions") {
                modifier.onClick { viewModel.resetPositions() }.width(Grow.Std).margin(end = sizes.smallGap)
            }
            Button("Reset Parameters") {
                modifier.onClick { viewModel.resetParameters() }.width(Grow.Std)
            }
        }
    }
}
