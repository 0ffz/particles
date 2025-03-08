package me.dvyy.particles

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.KoolConfigJvm.Backend
import de.fabmax.kool.math.Vec2i
import me.dvyy.particles.compute.forces.Force
import me.dvyy.particles.compute.forces.PairwiseForce

actual fun launchParticles(forces: List<Force>) = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = "Particles",
        isVsync = false,
        maxFrameRate = 500,
        renderBackend = Backend.OPEN_GL,
        windowSize = Vec2i(1920, 1080)
    )
) {
    launchApp(ctx, forces)
}
