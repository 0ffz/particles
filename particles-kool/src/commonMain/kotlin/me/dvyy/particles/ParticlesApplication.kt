package me.dvyy.particles

import me.dvyy.particles.compute.forces.Force
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun launchParticles(forces: List<Force>, uiModule: () -> Module = { module { } }, args: Array<String>)
