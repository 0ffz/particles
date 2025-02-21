import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.KoolConfigJvm.Backend
import de.fabmax.kool.math.Vec2i
import me.dvyy.particles.launchApp

/**
 * JVM main function / app entry point: Creates a new KoolContext (with optional platform-specific configuration) and
 * forwards it to the common-code launcher.
 */
fun main() = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = "Particles",
        isVsync = false,
        maxFrameRate = 500,
        renderBackend = Backend.OPEN_GL,
        windowSize = Vec2i(1920, 1080)
    )
) {
    launchApp(ctx)
}
