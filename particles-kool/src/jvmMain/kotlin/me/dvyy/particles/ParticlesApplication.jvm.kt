package me.dvyy.particles

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.KoolConfigJvm.Backend
import de.fabmax.kool.math.Vec2i
import me.dvyy.particles.compute.forces.Force

actual fun launchParticles(forces: List<Force>, args: Array<String>) {
    KoolApplication(
        config = KoolConfigJvm(
            windowTitle = "Particle HIVE",
            isVsync = false,
            maxFrameRate = 500,
            renderBackend = Backend.VULKAN,// else Backend.VULKAN,
            windowSize = Vec2i(1920, 1080)
        )
    ) {
        launchApp(ctx, forces)
    }
}
