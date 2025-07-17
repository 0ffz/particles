package me.dvyy.particles

import de.fabmax.kool.KoolContext
import me.dvyy.particles.clustering.ParticleClustering
import me.dvyy.particles.compute.ConvertParticlesShader
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.partitioning.GPUSort
import me.dvyy.particles.compute.partitioning.OffsetsShader
import me.dvyy.particles.compute.simulation.FieldsMultiPasses
import me.dvyy.particles.compute.simulation.FieldsShader
import me.dvyy.particles.compute.simulation.VerletHalfStepShader
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.ParticlesMesh
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.viewmodels.ForceParametersViewModel
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** This module persists across application reloads (i.e. the reload button) */
fun persistentModule(ctx: KoolContext) = module {
    single<KoolContext> { ctx }
    singleOf(::AppSettings)
    singleOf(::ConfigRepository)
    singleOf(::ParameterOverrides)
}

fun dataModule() = module {
    singleOf(::ParticleBuffers)
    singleOf(::CameraManager)
    singleOf(::ParticlesViewModel)
    singleOf(::ForceParametersViewModel)
    singleOf(::AppUI)
    singleOf(::ParticlesMesh)
    singleOf(::ParticleClustering)
}

fun shadersModule() = module {
    singleOf(::ConvertParticlesShader)
    singleOf(::OffsetsShader)
    singleOf(::GPUSort)
    singleOf(::FieldsShader)
    singleOf(::VerletHalfStepShader)
    singleOf(::FieldsMultiPasses)
}

fun sceneModule() = module {
    singleOf(::ParticlesScene)
}
