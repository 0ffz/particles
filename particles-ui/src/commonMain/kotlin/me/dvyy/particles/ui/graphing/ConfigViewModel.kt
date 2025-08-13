package me.dvyy.particles.ui.graphing

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.helpers.FileSystemUtils

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

    val fileTree: Flow<List<PlatformFile>> = configRepo.currentFile.map {
        FileSystemUtils.walkPathOrNull(it ?: return@map listOf())
    }
}
