package me.dvyy.particles

import OffsetsShader
import com.charleskorn.kaml.Yaml
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.math.spatial.toBoundingBoxD
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.OrbitInputTransform
import de.fabmax.kool.scene.addLineMesh
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.dvyy.particles.compute.ConvertParticlesShader
import me.dvyy.particles.compute.FieldsShader
import me.dvyy.particles.compute.GPUSort
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.render.Meshes
import me.dvyy.particles.ui.AppState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.UniformParameters
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.FieldParamsWindow
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.math.sqrt

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
//    val count: Int = (64 / 64) * 64
    val appScope = CoroutineScope(Dispatchers.RenderLoop)
    val parameters = YamlParameters(path = "parameters.yml", scope = appScope)
    val config = Yaml.default.decodeFromString(ParticlesConfig.serializer(), FileSystemUtils.read("particles.yml"))
    val state = AppState(parameters, appScope)
    val uniforms = UniformParameters(parameters, config)

    val count = state.count//state.targetCount.value
    val width = state.width.value
    val height = state.height.value
    val depth = if (state.threeDimensions.value) state.depth.value else 0

    val gridSize = run {
        val smallestSize = state.minGridSize.value
        val cols = (width / smallestSize).toInt()
        val rows = (height / smallestSize).toInt()
        val depths = if (depth == 0) 1 else (depth / smallestSize).toInt()
        if (rows * cols * depths > count) {
            sqrt((width.toFloat() * height.toFloat() * (depth.coerceAtLeast(1)).toFloat()) / count) + 1.0
        } else smallestSize
    }.toFloat()

    val passesPerFrame = state.passesPerFrame

    val gridCols = (width / gridSize).toInt().also { println("$it cols") }
    val gridRows = (height / gridSize).toInt().also { println("$it rows") }
    val gridDepth = (depth / gridSize).toInt().also { println("$it rows") }

    val buffers = FieldsBuffers(config.particles, width, height, depth, count)

    val application = startKoin {
        modules(module {
            single { parameters }
            single { state }
            single { uniforms }
            single { buffers }
            single<KoolContext> { ctx }
        }, module {
            singleOf(::ParticlesViewModel)
            singleOf(::FieldParamsWindow)
            singleOf(::AppUI)
        })
    }
    val appScene = scene {
        var swapIndex = 0
        // COMPUTE
        val sorting = ComputePass("Particles Compute")

        // Reset keys and indices based on grid cell particle is in
        val reset = GPUSort.resetBuffersShader.apply {
            uniform1f("gridSize", gridSize)
            uniform1i("gridRows", gridRows)
            uniform1i("gridCols", gridCols)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("indices", buffers.sortIndices)
            storage1d("positions", buffers.positionBuffer)
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
        val boxMax = Vec3f(gridSize * gridCols, gridSize * gridRows, gridSize * gridDepth)
        val fields = FieldsShader(config).also {
            it.gridSize = gridSize
            it.gridRows = gridRows
            it.gridCols = gridCols
            it.count = count
            it.colors = buffers.colorsBuffer
            it.particleTypes = buffers.particleTypesBuffer
            it.cellOffsets = buffers.offsetsBuffer
            it.particle2CellKey = buffers.particleGridCellKeys
            it.positions = buffers.positionBuffer
            it.velocities = buffers.velocitiesBuffer
            it.forces = buffers.forcesBuffer
            it.boxMax = boxMax

            it.halfStep_positions = buffers.positionBuffer
            it.halfStep_velocities = buffers.velocitiesBuffer
            it.halfStep_forces = buffers.forcesBuffer
            it.halfStep_boxMax = boxMax
        }

        repeat(passesPerFrame.value) { passIndex ->
            sorting.addTask(fields.halfStep, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
                onBeforeDispatch {
                    fields.halfStep_dT = state.dT.value
                }
            }
            sorting.addTask(fields.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {
                onBeforeDispatch {
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

        val convertShader = ConvertParticlesShader()
        sorting.addTask(convertShader.shader, numGroups = Vec3i(count / WORK_GROUP_SIZE, 1, 1)).apply {

            val conversionBuffer = Buffers.integers(config.particles.size)
            val conversionChances = Buffers.floats(config.particles.size)
            config.particles.forEachIndexed { id, from ->
                val to = from.convertTo
                if(to != null) {
                    conversionBuffer[id] = config.particleIds[to.type]!!.id.toInt()
                    conversionChances[id] = to.chance.toFloat()
                }
            }
            convertShader.convertTo = conversionBuffer
            convertShader.convertChances = conversionChances
            convertShader.particleTypes = buffers.particleTypesBuffer
            onBeforeDispatch {
                val shouldRun = Time.frameCount % 1000 == 0
                setNumGroupsByInvocations(if(shouldRun) count else 0, 1, 1)
                convertShader.randomSeed = 1000f + (Time.gameTime % 1000.0).toFloat()
            }
        }

//         RENDERING
        val bb = BoundingBoxF(Vec3f.ZERO, boxMax.times(Vec3f(1f, -1f, 1f)))

        if (state.threeDimensions.value) orbitCamera {
            maxZoom = width.toDouble()
            minZoom = 1.0
            zoom = width.toDouble() / 2
            zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
            translationBounds = bb.toBoundingBoxD().expand(Vec3d(500.0))
            setTranslation(bb.center.x.toDouble(), bb.center.y.toDouble(), bb.center.z.toDouble())
        }
        else {
            this.clearColor = ClearColorFill(Color("444444"))
            orbitCamera {
                maxZoom = width.toDouble()
                minZoom = 1.0
                leftDragMethod = OrbitInputTransform.DragMethod.PAN
                middleDragMethod = OrbitInputTransform.DragMethod.ROTATE
                zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
                zoom = width.toDouble() / 2
                translationBounds = bb.toBoundingBoxD().expand(Vec3d(500.0, 500.0, 0.0))
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
        onUpdate {
            iterations++
            if (iterations % 90 * 5 == 0) launchOnMainThread {
                return@launchOnMainThread
                buffers.particleTypesBuffer.readbackBuffer()
                buffers.positionBuffer.readbackBuffer()
                buffers.velocitiesBuffer.readbackBuffer()
                buffers.particleGridCellKeys.readbackBuffer()
                buffers.sortIndices.readbackBuffer()
                buffers.offsetsBuffer.readbackBuffer()
                println("Positions: " + (0 until count).map { buffers.positionBuffer.getF4(it) }.toString())
                println("Velocities: " + (0 until count).map { buffers.velocitiesBuffer.getF4(it) }.toString())
                println("Keys: " + (0 until count).map { buffers.particleGridCellKeys.getI1(it) }.toString())
                println("Indices: " + (0 until count).map { buffers.sortIndices.getI1(it) }.toString())
                println("Offsets: " + (0 until count).map { buffers.offsetsBuffer.getI1(it) }.toString())
            }
        }
    }
    ctx.scenes += appScene
    ctx.scenes += application.koin.get<AppUI>().ui
    ctx.scenes += debugOverlay()
}
