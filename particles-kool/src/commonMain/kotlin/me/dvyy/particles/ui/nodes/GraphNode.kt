package me.dvyy.particles.ui.nodes

import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ui2.Ui2Shader
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiRenderer
import de.fabmax.kool.pipeline.GpuType
import de.fabmax.kool.pipeline.StorageBuffer
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.scene.geometry.Usage
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Float32Buffer
import kotlinx.coroutines.Deferred
import me.dvyy.particles.compute.forces.ForceWithParameters
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import kotlin.math.min

class GraphNode(val layerName: String) : UiRenderer<UiNode> {
    private val graphMesh: Mesh
    private val graphGeom = IndexedVertexList(Ui2Shader.UI_MESH_ATTRIBS)
    private val graphBuilder = MeshBuilder(graphGeom).apply { isInvertFaceOrientation = true }
    private val clipBounds = MutableVec4f()
    private var width = 0
    private var height = 0
    private var valuesX = floatArrayOf()
    private var valuesY = floatArrayOf()
    private var viewport = MutableVec4f(-0.1f, -0.1f, 1f, 1f) // graph -> ui node positions
    private var updated = true
    var style: GraphStyle = GraphStyle.Line(Color.WHITE)

    fun Vec2f.toUi(): Vec2f {
        val vWidth = viewport.z - viewport.x
        val vHeight = viewport.w - viewport.y
        return Vec2f(
            width * ((x - viewport.x) / vWidth),
            height - height * ((y - viewport.y) / vHeight)
        )
    }

    suspend fun renderGpuFunction(scene: Scene, force: ForceWithParameters<PairwiseForce>) {
        val valuesX = FloatArray(256) { it.toFloat() / 256 }
        val valuesY = getOneShotResultsFor(scene, force).await()
        render(valuesX, valuesY)
    }

    fun getOneShotResultsFor(scene: Scene, force: ForceWithParameters<PairwiseForce>): Deferred<FloatArray> {
        val resolution = 256
        val outputBuffer = StorageBuffer(GpuType.Float1, size = resolution)
        val shader = force.createPairwiseForceComputeShader().apply {
            uniform1f("localNeighbors", 0f)
            uniform1i("lastIndex", resolution)
            force.uploadParameters()
            storage("distances", StorageBuffer(GpuType.Float1, size = resolution).apply {
                uploadData(Float32Buffer(resolution).apply {
                    repeat(resolution) {
                        put(5f * it.toFloat() / resolution)
                    }
                })
            })
            storage("outputBuffer", outputBuffer)
        }
        return execShader(
            scene,
            shader,
            numGroups = Vec3i((resolution + WORK_GROUP_SIZE) / WORK_GROUP_SIZE, 1, 1)
        ) {
            val result = Float32Buffer(resolution)
            outputBuffer.downloadData(result)
            result.toArray()
        }
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
        updateViewPort()
        updated = true
    }

    fun updateViewPort() {
        viewport.set(
            valuesX.min(),
            min(0f, valuesY.min()),
            valuesX.max(),
            valuesY.max(),
        )
    }

    fun pushNewValueRight(value: Float) {
        for (i in 0..<valuesY.lastIndex) {
            valuesY[i] = valuesY[i + 1]
        }
        valuesY[valuesY.lastIndex] = value
        updateViewPort()
        updated = true
    }

    fun clearYAxis(value: Float = 0f) {
        for(i in valuesY.indices) {
            valuesY[i] = value
        }
        updated = true
    }

    init {
        graphMesh = Mesh(graphGeom, name = "GraphNode")
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
        node.surface.getMeshLayer(node.modifier.zLayer - 1).addCustomLayer("dt-graph-${layerName}") { graphMesh }
        if (node.clipBoundsPx == clipBounds && !updated) return
        clipBounds.set(node.clipBoundsPx)
        updated = false
        node.apply {
            width = widthPx.toInt()
            height = heightPx.toInt()
            graphBuilder.clear()
            if (valuesX.isEmpty() || valuesY.isEmpty()) return@apply
            when (val style = style) {
                is GraphStyle.Line -> {
                    graphBuilder.configured(style.color) {
                        var prev = Vec2f(valuesX[0], valuesY[0]).toUi()
                        for (i in 1..valuesX.lastIndex) {
                            val new = Vec2f(valuesX[i], valuesY[i]).toUi()
                            line(prev, new, 1f)
                            prev = new
                        }
                    }
                }

                is GraphStyle.Bar -> {
                    graphBuilder.configured(Color.WHITE) {
                        val width = style.width.toFloat()
                        valuesX.zip(valuesY).forEach { (x, y) ->
                            val bottom = Vec2f(x, 0f).toUi()//.plus(Vec2f(width /2, 0f))
                            val top = Vec2f(x, y).toUi()//.plus(Vec2f(width /2, 0f))
                            line(bottom, top, width)
                        }
                    }
                }
            }
            // Render background grid
            graphBuilder.configured(Color.GRAY) {
                // horizontal
                line(Vec2f(0f, 0f).toUi(), Vec2f(valuesX.max(), 0f).toUi(), 1f)
                line(Vec2f(0f, 0f).toUi(), Vec2f(0f, valuesY.max()).toUi(), 1f)
            }
        }
    }
//
//override fun renderUi(node: UiNode) {
//        node.apply {
//            getTextBuilder(sizes.normalText).configured(Color.WHITE) {
//                text(TextProps(sizes.normalText).apply {
//                    text = "Hello world"; scale = 2f; isYAxisUp = false
//                    this.origin.set(0f, 70f, 0f)
//                })
//            }
//        }
//    if (node.clipBoundsPx != clipBounds || updated) {
//        node.surface.getMeshLayer(node.modifier.zLayer + 1)
//        clipBounds.set(node.clipBoundsPx)
//        updated = false
//        println("Ran render ui!")
//        node.apply {
//        }
//        node.surface.getMeshLayer(node.modifier.zLayer - 1).addCustomLayer("dt-graph") { graphMesh }
//    }
//}
}
