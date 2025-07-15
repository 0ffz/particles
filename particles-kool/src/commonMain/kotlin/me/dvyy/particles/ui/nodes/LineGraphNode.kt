package me.dvyy.particles.ui.nodes

import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ui2.Ui2Shader
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiRenderer
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.scene.geometry.Usage
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE

class LineGraphNode : UiRenderer<UiNode> {
    private val graphMesh: Mesh
    private val graphGeom = IndexedVertexList(Ui2Shader.Companion.UI_MESH_ATTRIBS)
    private val graphBuilder = MeshBuilder(graphGeom).apply { isInvertFaceOrientation = true }
    private val clipBounds = MutableVec4f()
    private var width = 0
    private var height = 0
    private var valuesX = floatArrayOf()
    private var valuesY = floatArrayOf()
    private var viewport = MutableVec4f(-0.1f, -0.1f, 1f, 1f) // graph -> ui node positions
    private var updated = true

    fun Vec2f.toUi(): Vec2f {
        val vWidth = viewport.z - viewport.x
        val vHeight = viewport.w - viewport.y
        return Vec2f(
            width * ((x - viewport.x) / vWidth),
            height - height * ((y - viewport.y) / vHeight)
        )
    }

    suspend fun renderGpuFunction(scene: Scene, force: PairwiseForce) {
        val valuesX = FloatArray(256) { it.toFloat() / 256 }
        val valuesY = getOneShotResultsFor(scene, force).await()
        render(valuesX, valuesY)
    }

    fun getOneShotResultsFor(scene: Scene, force: PairwiseForce): Deferred<FloatArray> {
        val computePass = ComputePass("single-shot")
        val resolution = 256
        val shader = force.createForceComputeShader()
        shader.uniform1f("localNeighbors", 1f)
        shader.uniform1i("lastIndex", resolution)
        shader.storage("distances", StorageBuffer(GpuType.Float1, size = resolution).apply {
            uploadData(Float32Buffer(resolution).apply {
                repeat(resolution) {
                    put(5f * it.toFloat() / resolution)
                }
            })
        })
        val outputBuffer = StorageBuffer(GpuType.Float1, size = resolution)
        shader.storage("outputBuffer", outputBuffer)
        val task = computePass.addTask(shader, Vec3i((resolution + WORK_GROUP_SIZE) / WORK_GROUP_SIZE, 1, 1))
        scene.addComputePass(computePass)

        val deferred = CompletableDeferred<FloatArray>()
        task.onAfterDispatch {
            task.isEnabled = false
            try {
                launchOnMainThread {
                    val result = Float32Buffer(resolution)
                    outputBuffer.downloadData(result)
                    deferred.complete(result.toArray())
                }
            } catch (e: Exception) {
                println(e.stackTraceToString())
            } finally {
                scene.removeComputePass(computePass)
            }
        }

        return deferred
    }

    fun renderFunction(
        from: Float,
        to: Float,
        resolution: Int = 100,
        function: (Float) -> Float,
    ) {
        val valuesX = FloatArray(resolution) { from + ((it.toFloat() / resolution) * (to - from)) }
        val valuesY = FloatArray(resolution) {
            val x = from + ((it.toFloat() / resolution) * (to - from))
            function(x)
        }
        render(valuesX, valuesY)
    }

    fun render(valuesX: FloatArray, valuesY: FloatArray) {
        this.valuesX = valuesX
        this.valuesY = valuesY
        viewport.set(
            valuesX.min(),
            valuesY.min(),
            valuesX.max(),
            valuesY.max(),
        )
        updated = true
    }

    init {
        graphMesh = Mesh(graphGeom, name = "DebugOverlay/DeltaTGraph")
        graphMesh.geometry.usage = Usage.DYNAMIC
        graphMesh.shader = Ui2Shader()
    }

    override fun renderUi(node: UiNode) {
//        node.apply {
//            getTextBuilder(sizes.normalText).configured(Color.WHITE) {
//                text(TextProps(sizes.normalText).apply {
//                    text = "Hello world"; scale = 2f; isYAxisUp = false
//                    this.origin.set(0f, 70f, 0f)
//                })
//            }
//        }
        if (node.clipBoundsPx != clipBounds || updated) {
            node.surface.getMeshLayer(node.modifier.zLayer + 1)
            clipBounds.set(node.clipBoundsPx)
            updated = false
            println("Ran render ui!")
            node.apply {
                width = widthPx.toInt()
                height = heightPx.toInt()
                graphBuilder.clear()
                // Render graph
                if (valuesX.isEmpty() || valuesY.isEmpty()) return@apply
                graphBuilder.configured(Color.Companion.WHITE) {
                    var prev = Vec2f(valuesX[0], valuesY[0]).toUi()
                    for (i in 1..valuesX.lastIndex) {
                        val new = Vec2f(valuesX[i], valuesY[i]).toUi()
                        line(prev, new, 1f)
                        prev = new
                    }
                }
                // Render background grid
                graphBuilder.configured(Color.Companion.GRAY) {
                    // horizontal
                    line(Vec2f(0f, 0f).toUi(), Vec2f(valuesX.max(), 0f).toUi(), 1f)
                    line(Vec2f(0f, 0f).toUi(), Vec2f(0f, valuesY.max()).toUi(), 1f)
                }
            }
        }
        node.surface.getMeshLayer(node.modifier.zLayer - 1).addCustomLayer("dt-graph") { graphMesh }
    }
}
