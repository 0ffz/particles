package me.dvyy.particles

import OffsetsShader
import com.charleskorn.kaml.Yaml
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.OrbitInputTransform
import de.fabmax.kool.scene.addLineMesh
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.debugOverlay
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.render.Meshes
import me.dvyy.particles.ui.FieldOptions
import me.dvyy.particles.ui.FieldsState
import me.dvyy.particles.ui.UniformParameters
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
//    val count: Int = (64 / 64) * 64
    val appScope = CoroutineScope(Dispatchers.RenderLoop)
    val parameters = YamlParameters(path = "../assets/parameters.yml", scope = appScope)
    val config =
        Yaml.default.decodeFromString(ParticlesConfig.serializer(), FileSystemUtils.read("../assets/particles.yml"))
    val state = FieldsState(parameters, appScope)
    val uniforms = UniformParameters(parameters, config)

    val count = (state.targetCount.value / 64) * 64
    val width = state.width.value
    val height = state.height.value
    val depth = if (state.threeDimensions.value) state.depth.value else 0

    val gridSize = run {
        val smallestSize = state.minGridSize.value
        val cols = (width / smallestSize).toInt()
        val rows = (height / smallestSize).toInt()
        if (rows * cols > count) {
            sqrt((width.toFloat() * height.toFloat()) / count) + 1.0
        } else smallestSize
    }.toFloat()

    val passesPerFrame = state.passesPerFrame

    val gridCols = (width / gridSize).toInt().also { println("$it cols") }
    val gridRows = (height / gridSize).toInt().also { println("$it rows") }
    val gridDepth = (depth / gridSize).toInt().also { println("$it rows") }

    val buffers = FieldsBuffers(config.particles, width, height, depth, count)

    ctx.scenes += scene {
        var swapIndex = 0
        // COMPUTE
        val sorting = ComputePass("Particles Compute")

        // Reset keys and indices based on grid cell particle is in
        val reset = GPUSort.resetBuffersShader.apply {
            uniform1f("gridSize", gridSize)
            uniform1i("gridCols", gridCols)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("indices", buffers.sortIndices)
            storage1d("positions", buffers.positionBuffers[0])
        }
        sorting.addTask(reset, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1))

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

        val fields = FieldsShader(config).also {
            it.gridSize = gridSize
            it.gridRows = gridRows
            it.gridDepth = gridDepth
            it.gridCols = gridCols
            it.count = count
            it.colors = buffers.colorsBuffer
            it.particleTypes = buffers.particleTypesBuffer
            it.cellOffsets = buffers.offsetsBuffer
            it.particle2CellKey = buffers.particleGridCellKeys
            it.prevForces = buffers.prevForcesBuffer
        }
        repeat(passesPerFrame.value) { passIndex ->
            sorting.addTask(fields.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
                pipeline.swapPipelineData("fieldsPass$passIndex")
                fields.prevPositions = buffers.positionBuffers[passIndex % 2]
                fields.currPositions = buffers.positionBuffers[(passIndex + 1) % 2]
                fields.prevVelocities = buffers.velocitiesBuffers[passIndex % 2]
                fields.currVelocities = buffers.velocitiesBuffers[(passIndex + 1) % 2]
                fields.currVelocities = buffers.velocitiesBuffers[(passIndex + 1) % 2]

                onBeforeDispatch {
                    pipeline.swapPipelineData("fieldsPass$passIndex")
                    fields.dT = state.dT.value
                    fields.maxVelocity = state.maxVelocity.value
                    fields.maxForce = state.maxForce.value
                    uniforms.uniformParams.forEach { (param, value) ->
                        fields.shader.uniform1f(param.uniformName).set(value.value)
                    }
                    swapIndex++
                }
            }
            addComputePass(sorting)
        }

//         RENDERING
        val bb = BoundingBoxF(
            Vec3f(0f),
            Vec3f(gridSize * gridCols, -gridSize * gridRows, gridSize * gridDepth),
        )

        if (state.threeDimensions.value) orbitCamera {
            maxZoom = width.toDouble()
            minZoom = 1.0
            zoom = width.toDouble() / 2
            setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
        }
        else {
            this.clearColor = ClearColorFill(Color("444444"))
            orbitCamera {
                maxZoom = width.toDouble()
                minZoom = 1.0
                leftDragMethod = OrbitInputTransform.DragMethod.PAN
                middleDragMethod = OrbitInputTransform.DragMethod.ROTATE
                zoom = width.toDouble() / 2
                setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
            }
        }
        val instances = Meshes.particleMeshInstances(count)
        addNode(
            Meshes.particleMesh(
                buffers,
                instances
            )
        )


        addLineMesh {
            addBoundingBox(bb, Color.WHITE)
        }
//        onRelease {
//            positionsBuffer.release()
//        }
        var iterations = 0
//        launchOnMainThread {
//            while(true) {
//                config.particles.forEach { from ->
//                    from.convertTo.forEach { (to, options) ->
//                        options.every
//                        delay()
//                    }
//                }
//            }
//        }
        onUpdate {
            iterations++
            if (iterations % 90 * 5 == 0) launchOnMainThread {
                buffers.particleTypesBuffer.readbackBuffer()
                return@launchOnMainThread
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

    ctx.scenes += FieldOptions(
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
        state = state,
        ctx = ctx,
        passesPerFrame = passesPerFrame.value,
        uniforms = uniforms,
    ).ui
    ctx.scenes += debugOverlay()
}
