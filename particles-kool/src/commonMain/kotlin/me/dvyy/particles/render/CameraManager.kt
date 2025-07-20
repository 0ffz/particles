package me.dvyy.particles.render

import de.fabmax.kool.math.Vec2f
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
    private val gridMesh = mutableStateOf<LineMesh?>(null)

    fun manageCameraFor(scene: Scene) {
        scope.launch {
            configRepo.config.distinctUntilChangedBy { it.simulation.threeDimensions }.collect {
                updateCamera(scene, it)
            }
        }
    }

    fun Scene.updateGridMesh() {
        gridMesh.set(addLineMesh {
            generate {
                color = Color.GRAY
                val cells = configRepo.gridCells
                val gridSize = configRepo.gridSize
                repeat(cells.y) {
                    val height = -it * gridSize
                    val max = cells.x * gridSize
                    line(Vec2f(0f, height), Vec2f(max, height), 0f)
                }
                repeat(cells.x) {
                    val height = it * gridSize
                    val max = cells.y * gridSize
                    line(Vec2f(height, 0f), Vec2f(height, -max), 0f)
                }
//                line(Vec2f(0f, 0f), Vec2f(boxMax.x, 0f), 1f)
            }
//            shader = KslUnlitShader {
//                color { constColor(Color.RED) }
//            }
        })
    }

    fun updateCamera(scene: Scene, config: ParticlesConfig) {
        val boxMax = configRepo.boxSize
        val bb = BoundingBoxF(Vec3f.ZERO, boxMax.times(Vec3f(1f, -1f, 1f)))
        lineMesh.value?.let { scene.removeNode(it) }
        lineMesh.set(scene.addLineMesh {
            addBoundingBox(bb, Color.WHITE)
        })
        scene.updateGridMesh()
        scene.clearColor = ClearColorFill(Color("444444"))
        scene.orbitCamera {
            maxZoom = boxMax.length().toDouble()
            minZoom = 1.0
            if (!config.simulation.threeDimensions) {
                leftDragMethod = OrbitInputTransform.DragMethod.PAN
                middleDragMethod = OrbitInputTransform.DragMethod.ROTATE
            }
            zoom = boxMax.length().toDouble() / 2
            zoomAnimator.set(boxMax.length().toDouble() / 2) // skip zoom animation
            zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
            translation.set(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
            translationBounds = bb.toBoundingBoxD().expand(Vec3d(500.0))
        }
    }
}
