package me.dvyy.particles

import OffsetsShader
import ReorderBuffersShader
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.debugOverlay
import de.fabmax.kool.util.launchOnMainThread
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.render.Meshes
import kotlin.math.sqrt

class FieldsBuffers(
    val width: Int,
    val height: Int,
    val count: Int,
) {
    val positionBuffers = arrayOf(
        Buffers.positions(count, width, height),
        Buffers.positions(count, width, height)
    )
    val velocitiesBuffers = arrayOf(
        Buffers.velocities(count, 20.0),
        Buffers.velocities(count, 20.0),
    )
    val particleGridCellKeys = Buffers.integers(count)/*.apply {
        for (i in 0 until count) this[i] = count - i - 1//Random.nextInt(count)
    }*/
    val sortIndices = Buffers.integers(count)/*.apply {
        for (i in 0 until count) this[i] = i
    }*/
    val offsetsBuffer = Buffers.integers(count)
    val particleTypesBuffer = Buffers.integers(count)
    val colorsBuffer = Buffers.float4(count)
}
/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
//    val count: Int = (64 / 64) * 64
    val count = 1_0000
    val width = ctx.windowWidth
    val height = ctx.windowHeight
    val minGridSize = 5.0
    val gridSize = run {
        val smallestSize = minGridSize
        val cols = (width / smallestSize).toInt()
        val rows = (height / smallestSize).toInt()
        if (rows * cols > count) {
            sqrt((width.toFloat() * height.toFloat()) / count) + 1.0
        } else smallestSize
    }.toFloat()
    val gridCols = (width / gridSize).toInt().also { println("$it cols") }
    val gridRows = (height / gridSize).toInt().also { println("$it rows") }
    val buffers = FieldsBuffers(width, height, count)
    ctx.scenes += scene {
        var swapIndex = 0
        // COMPUTE
        val sorting = ComputePass("Particles Compute")

        // Swapping grid information at each step
        fun currPositions() = buffers.positionBuffers[swapIndex % 2]
        fun prevPositions() = buffers.positionBuffers[(swapIndex + 1) % 2]
        fun currVelocities() = buffers.velocitiesBuffers[swapIndex % 2]
        fun prevVelocities() = buffers.velocitiesBuffers[(swapIndex + 1) % 2]

        // Reset keys and indices based on grid cell particle is in
        val reset = GPUSort.resetBuffersShader.apply {
            uniform1f("gridSize", gridSize)
            uniform1i("gridCols", gridCols)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("indices", buffers.sortIndices)
        }
        sorting.addTask(reset, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
            onBeforeDispatch {
                reset.storage1d("positions", currPositions())
            }
        }

        // Sort by grid cells
        gpuSorting(count, buffers = buffers, computePass = sorting)

//        val reorderBuffers = ReorderBuffersShader().also {
//            it.indices = sortIndices
//            it.numValues = count
//        }
//        sorting.addTask(reorderBuffers.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
////            pipeline.swapPipelineData("Curr")
////            reorderBuffers.positions = currPositions()
////            reorderBuffers.velocities = currVelocities()
//            onBeforeDispatch {
////                pipeline.swapPipelineData("Curr")
//                reorderBuffers.positions = currPositions()
//                reorderBuffers.velocities = currVelocities()
//            }
//        }
//        sorting.addTask(reorderBuffers.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
////            pipeline.swapPipelineData("Prev")
////            reorderBuffers.positions = prevPositions()
////            reorderBuffers.velocities = prevVelocities()
//            onBeforeDispatch {
////                pipeline.swapPipelineData("Prev")
//                reorderBuffers.positions = prevPositions()
//                reorderBuffers.velocities = prevVelocities()
//            }
//        }
        val offsets = OffsetsShader.apply {
            uniform1i("numValues", count)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("offsets", buffers.offsetsBuffer)
        }
        sorting.addTask(offsets, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
            onBeforeDispatch {
//                offsets.storage1d("keys", particleGridCellKeys)
//                offsets.storage1d("offsets", offsetsBuffer)
            }
        }

        val fields = FieldsShader().also {
            it.gridSize = gridSize
            it.gridRows = gridRows
            it.gridCols = gridCols
            it.epsilon = 10f
            it.sigma = 5f
            it.dT = 0.01f
            it.count = count
            it.maxForce = 10000f
            it.maxVelocity = 20f
            it.colors = buffers.colorsBuffer
        }

        sorting.addTask(fields.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
            onBeforeDispatch {
                fields.prevPositions = prevPositions()
                fields.currPositions = currPositions()
                fields.prevVelocities = prevVelocities()
                fields.currVelocities = currVelocities()
                fields.particleTypes = buffers.particleTypesBuffer
                fields.cellOffsets = buffers.offsetsBuffer
                fields.particle2CellKey = buffers.particleGridCellKeys
                swapIndex++
            }
        }
        addComputePass(sorting)

        // RENDERING
        setupUiScene(clearColor = ClearColorFill(Color("444444")))
//        orbitCamera {
//            minZoom = 0.0001
//            zoom = 0.001
//        }
        val instances = Meshes.particleMeshInstances(count)
        addNode(Meshes.particleMesh(buffers.positionBuffers.first(), buffers.colorsBuffer, instances))
//        onRelease {
//            positionsBuffer.release()
//        }
        var iterations = 0
        onUpdate {
            iterations++
            if(iterations % 90 * 5 == 0) launchOnMainThread {
                return@launchOnMainThread
//            removeComputePass(sorting)
                buffers.positionBuffers[0].readbackBuffer()
                buffers.velocitiesBuffers[0].readbackBuffer()
                buffers.particleGridCellKeys.readbackBuffer()
                buffers.sortIndices.readbackBuffer()
                buffers.offsetsBuffer.readbackBuffer()
                println("Positions: " + (0 until count).map { buffers.positionBuffers[0].getF4(it) }.toString())
                println("Velocities: " + (0 until count).map { buffers.velocitiesBuffers[0].getF4(it) }.toString())
                println("Keys: " + (0 until count).map { buffers.particleGridCellKeys.getI1(it) }.toString())
                println("Indices: " + (0 until count).map { buffers.sortIndices.getI1(it) }.toString())
                println("Offsets: " + (0 until count).map { buffers.offsetsBuffer.getI1(it) }.toString())
            }
        }

    }

    ctx.scenes += debugOverlay()
}
