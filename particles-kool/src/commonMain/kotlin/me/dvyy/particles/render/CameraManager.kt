package me.dvyy.particles.render

import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.math.spatial.toBoundingBoxD
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.ParticlesConfig

class CameraManager(
    val configRepo: ConfigRepository,
    val scope: CoroutineScope,
) {
    private val lineMesh = mutableStateOf<LineMesh?>(null)

    fun manageCameraFor(scene: Scene) {
        scope.launch {
            configRepo.config.distinctUntilChangedBy { it.simulation.threeDimensions }.collect {
                updateCamera(scene, it)
            }
        }
    }

    fun updateCamera(scene: Scene, config: ParticlesConfig) {
        val boxMax = configRepo.boxSize
        val bb = BoundingBoxF(Vec3f.ZERO, boxMax.times(Vec3f(1f, -1f, 1f)))
        lineMesh.value?.let { scene.removeNode(it) }
        lineMesh.set(scene.addLineMesh {
            addBoundingBox(bb, Color.WHITE)
        })
        if (config.simulation.threeDimensions) scene.orbitCamera {
            maxZoom = boxMax.x.toDouble()
            minZoom = 1.0
            zoom = boxMax.x.toDouble() / 2
            zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
            translationBounds = bb.toBoundingBoxD().expand(Vec3d(500.0))
            setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
        }
        else {
            scene.clearColor = ClearColorFill(Color("444444"))
            scene.orbitCamera {
                maxZoom = boxMax.x.toDouble()
                minZoom = 1.0
                leftDragMethod = OrbitInputTransform.DragMethod.PAN
                middleDragMethod = OrbitInputTransform.DragMethod.ROTATE
                zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
                zoom = boxMax.x.toDouble() / 2
                translationBounds = bb.toBoundingBoxD().expand(Vec3d(500.0, 500.0, 0.0))
                setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
            }
        }
    }
}
