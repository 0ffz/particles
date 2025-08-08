package me.dvyy.particles

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.backend.vk.RenderBackendVk
import me.dvyy.particles.compute.forces.Force
import org.koin.core.module.Module

actual fun launchParticles(forces: List<Force>, uiModule: () -> Module, args: Array<String>) {
    KoolApplication(
        config = KoolConfigJvm(
            windowTitle = "Particles",
            isVsync = false,
            maxFrameRate = 500,
            renderBackend = RenderBackendVk,
            windowSize = Vec2i(1920, 1080)
        )
    ) {
        launchApp(ctx, uiModule, forces)
    }
}
