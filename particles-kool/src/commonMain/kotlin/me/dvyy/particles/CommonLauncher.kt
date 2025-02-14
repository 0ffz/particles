package me.dvyy.particles

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.debugOverlay
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.compute.SimpleMovement
import me.dvyy.particles.render.Meshes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
    val count: Int = (64 / 64) * 64
    val width = ctx.windowWidth
    val height = ctx.windowHeight
    val positionsBuffer = Buffers.positions(count, width, height)
    val particleGridCellKeys = Buffers.integers(count)
    val sortIndices = Buffers.integers(count)

    ctx.scenes += scene {
        // COMPUTE
        val movement = SimpleMovement
        movement.storage1d("positionsBuffer", positionsBuffer)
        addComputePass(ComputePass(movement, count))
        // Sorts keys low to high, if indices are ordered 1 to count, as well as indices to match up new positions
        gpuSorting(count, keysBuffer = particleGridCellKeys, indicesBuffer = sortIndices)

        // RENDERING
        setupUiScene(clearColor = ClearColorFill(Color("444444")))
        val instances = Meshes.particleMeshInstances(count)
        addNode(Meshes.particleMesh(positionsBuffer, instances))

        onRelease {
            positionsBuffer.release()
        }
    }

    ctx.scenes += debugOverlay()
}
