package me.dvyy.particles

import OffsetsShader
import de.fabmax.kool.KoolContext
import me.dvyy.particles.clustering.ParticleClustering
import me.dvyy.particles.compute.ConvertParticlesShader
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.ParameterOverrides
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.ParticlesMesh
import me.dvyy.particles.ui.AppUI
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
    singleOf(::UniformParameters)
    singleOf(::ParticleBuffers)
    singleOf(::CameraManager)
    singleOf(::ParticlesViewModel)
    singleOf(::AppUI)
    singleOf(::ParticlesMesh)
    singleOf(::ParticleClustering)
}

fun shadersModule() = module {
    singleOf(::ConvertParticlesShader)
    singleOf(::OffsetsShader)
    singleOf(::GPUSort)
    singleOf(::FieldsShader)
}

fun sceneModule() = module {
    singleOf(::ParticlesScene)
}
