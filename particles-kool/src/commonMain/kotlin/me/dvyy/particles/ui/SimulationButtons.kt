package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.*
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.Subcategory

fun UiScope.SimulationButtons(
    viewModel: ParticlesViewModel,
    paramsChanged: Boolean,
) {
    Subcategory("Reset") {
        Row(Grow.Std) {
            modifier.padding(sizes.smallGap)
            Button("Positions") {
                modifier.onClick { viewModel.resetPositions() }.width(Grow.Std).margin(end = sizes.smallGap)
            }
            Button(if (paramsChanged) "(*) Parameters" else "Parameters") {
                modifier.onClick { viewModel.resetParameters() }.width(Grow.Std)
            }
        }
    }
}
