import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.KoolConfigJs.Backend
import de.fabmax.kool.modules.ksl.KslBlinnPhongShader
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.pipeline.backend.RenderBackend
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import template.launchApp

/**
 * JS main function / app entry point: Creates a new KoolContext (with optional platform-specific configuration) and
 * forwards it to the common-code launcher.
 */
fun main() = KoolApplication(
    config = KoolConfigJs(
        canvasName = "glCanvas",
        renderBackend = Backend.WEB_GPU,
        isGlobalKeyEventGrabbing = true,
        forceFloatDepthBuffer = false,
        deviceScaleLimit = 1.5,
    )
) {
//    ctx.scenes += scene {
//        addColorMesh {
//            generate {
//                cube {
//                    colored()
//                    size.set(3f, 3f, 3f)
//                }
//            }
//            shader = KslPbrShader {
//                color { vertexColor() }
//                metallic(0f)
//                roughness(0.25f)
//            }
//        }
//        defaultOrbitCamera()
//        return@scene
//    }
    launchApp(ctx)
}
