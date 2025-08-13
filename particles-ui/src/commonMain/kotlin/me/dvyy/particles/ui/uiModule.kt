package me.dvyy.particles.ui

import me.dvyy.particles.ui.graphing.ConfigViewModel
import me.dvyy.particles.ui.sidebar.TestViewModel
import me.dvyy.particles.ui.windows.ParticlesViewModel
import me.dvyy.particles.ui.windows.live_parameters.ForceParametersViewModel
import me.dvyy.particles.ui.windows.statistics.StatisticsViewModel
import me.dvyy.particles.ui.windows.visual_options.VisualOptionsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun uiModule() = module(createdAtStart = true) {
    singleOf(::ParticlesViewModel)
    singleOf(::ForceParametersViewModel)
    singleOf(::TestViewModel)
    singleOf(::VisualOptionsViewModel)
    singleOf(::ConfigViewModel)
    singleOf(::StatisticsViewModel)
    single { ParticlesUI(this) }
    single(named("ui-scene")) { get<ParticlesUI>().scene }
}
