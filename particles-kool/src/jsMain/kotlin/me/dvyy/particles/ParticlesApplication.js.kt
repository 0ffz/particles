package me.dvyy.particles

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.KoolConfigJs.Backend
import me.dvyy.particles.compute.forces.Force

actual fun launchParticles(forces: List<Force>, args: Array<String>) = KoolApplication(
    config = KoolConfigJs(
        canvasName = "glCanvas",
        renderBackend = Backend.WEB_GPU,
        isGlobalKeyEventGrabbing = true,
        forceFloatDepthBuffer = false,
        deviceScaleLimit = 1.5,
    )
) {
    launchApp(ctx, forces)
}

