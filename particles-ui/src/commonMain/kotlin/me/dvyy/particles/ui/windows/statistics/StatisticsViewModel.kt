package me.dvyy.particles.ui.windows.statistics

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.toString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.yield

class StatisticsViewModel(
    val scope: CoroutineScope
) {
    val fps = flow<String> {
        while (true) {
            emit(KoolSystem.requireContext().fps.toString(2))
            yield()
        }
    }.stateIn(scope, SharingStarted.Eagerly, "0")
}
