package me.dvyy.particles.ui.graphing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.Simulation

class ConfigViewModel(
    private val configRepo: ConfigRepository,
    private val appSettings: AppSettings,
    private val scope: CoroutineScope,
) {
    val simulation = configRepo.config
        .map { it.simulation }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Simulation())

    fun updateSimulation(update: Simulation.() -> Simulation) {
        val curr = configRepo.config.value
        configRepo.updateConfig(curr.copy(simulation = update(curr.simulation)))
    }
}
