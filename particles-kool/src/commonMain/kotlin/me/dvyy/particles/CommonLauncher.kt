package me.dvyy.particles

import OffsetsShader
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.addLineMesh
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.RenderLoopCoroutineDispatcher
import de.fabmax.kool.util.debugOverlay
import de.fabmax.kool.util.launchDelayed
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.render.Meshes
import me.dvyy.particles.ui.FieldOptions
import me.dvyy.particles.ui.FieldsState
import kotlin.math.sqrt
import kotlin.random.Random

class FieldsBuffers(
    val width: Int,
    val height: Int,
    val depth: Int,
    val count: Int,
) {
    val positionBuffers = arrayOf(
        Buffers.positions(count, width, height, depth),
        Buffers.positions(count, width, height, depth)
    )
    val velocitiesBuffers = arrayOf(
        Buffers.velocities(count, depth != 0, 20.0),
        Buffers.velocities(count, depth != 0, 20.0),
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
    val threeDimensions = false
    val count = (1_000_0 / 64) * 64
    val width = 1920//ctx.windowWidth
    val height = 1080//ctx.windowHeight
    val depth = if (threeDimensions) ctx.windowWidth else 0
    val minGridSize = 2.5 * 5.0
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
    val gridDepth = (depth / gridSize).toInt().also { println("$it rows") }
    val buffers = FieldsBuffers(width, height, depth, count)

    val appScope = CoroutineScope(Dispatchers.RenderLoop)
    val parameters = YamlParameters(path = Path("../assets/parameters.yml"), scope = appScope)
    val state = FieldsState(parameters, appScope)

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
//            onBeforeDispatch {
//                offsets.storage1d("keys", particleGridCellKeys)
//                offsets.storage1d("offsets", offsetsBuffer)
//            }
        }

        val fields = FieldsShader().also {
            it.gridSize = gridSize
            it.gridRows = gridRows
            it.gridDepth = gridDepth
            it.gridCols = gridCols
            it.epsilon = 100f
            it.sigma = 2f
            it.dT = 0.01f
            it.count = count
            it.maxForce = 100000f
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
                fields.epsilon = state.epsilon.value
                swapIndex++
            }
        }
        addComputePass(sorting)

//         RENDERING
        val bb = BoundingBoxF(
            Vec3f(0f),
            Vec3f(gridSize * gridCols, -gridSize * gridRows, gridSize * gridDepth),
        )

        if (threeDimensions) orbitCamera {
            maxZoom = 1000.0
            minZoom = 1.0
            zoom = 500.0
            setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
        }
        else setupUiScene(clearColor = ClearColorFill(Color("444444")))
        val instances = Meshes.particleMeshInstances(count)
        addNode(Meshes.particleMesh(buffers.positionBuffers.first(), buffers.colorsBuffer, instances))


        addLineMesh {
            addBoundingBox(bb, Color.WHITE)
        }
//        onRelease {
//            positionsBuffer.release()
//        }
        var iterations = 0
        onUpdate {
            iterations++
            if (iterations % 90 * 5 == 0) launchOnMainThread {
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

    ctx.scenes += UiScene("fields-options") {
        addNode(
            FieldOptions(
                resetPositions = {
                    launchOnMainThread {
                        buffers.positionBuffers.forEach {
                            for (i in 0 until count) {
                                it[i] = Vec4f(
                                    Random.Default.nextInt(width).toFloat(),
                                    Random.Default.nextInt(height).toFloat(),
                                    if (depth == 0) 0f else Random.Default.nextInt(depth).toFloat(),
                                    0f
                                )
                            }
                        }
                    }
                },
                load = {
                    launchOnMainThread {
                        parameters.load()
//                        SystemFileSystem.source(Path("../assets/parameters.yml")).buffered().readString().let { println(it) }
//                        Assets.loadBlob("parameters.yml").getOrThrow().decodeToString()
//                        FieldsState.fileText.set(asset.first().read().decodeToString().lineSequence().first())
                    }
                },
                save = {
                    launchOnMainThread {
                        parameters.save()
                    }
                },
                fieldsState = state
            ).dock
        )
    }
    ctx.scenes += debugOverlay()
}
