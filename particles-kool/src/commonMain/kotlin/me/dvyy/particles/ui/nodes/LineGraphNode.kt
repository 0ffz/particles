package me.dvyy.particles.ui.nodes

import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ui2.Ui2Shader
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiRenderer
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.scene.geometry.Usage
import de.fabmax.kool.util.Color
import me.dvyy.particles.compute.forces.PairwiseForce

class LineGraphNode : UiRenderer<UiNode> {
    private val graphMesh: Mesh
    private val graphGeom = IndexedVertexList(Ui2Shader.Companion.UI_MESH_ATTRIBS)
    private val graphBuilder = MeshBuilder(graphGeom).apply { isInvertFaceOrientation = true }
    private val clipBounds = MutableVec4f()
    private var width = 0
    private var height = 0
    private var valuesX = arrayOf<Float>()
    private var valuesY = arrayOf<Float>()
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

//    fun renderGpuFunction(force: PairwiseForce) {
//        val computePass = ComputePass("single-shot")
//        val task = computePass.addTask(force.createFunction(), groupDimensions)
//        scene.addComputePass(computePass)
//        task.onAfterDispatch {
//            scene.removeComputePass(computePass)
//            // or
//            task.isEnabled = false
//        }
//    }

    fun renderFunction(
        from: Float,
        to: Float,
        resolution: Int = 100,
        function: (Float) -> Float,
    ) {
        valuesX = Array(resolution) { from + ((it.toFloat() / resolution) * (to - from)) }
        valuesY = Array(resolution) {
            val x = from + ((it.toFloat() / resolution) * (to - from))
            function(x)
        }
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
