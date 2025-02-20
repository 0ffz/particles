package me.dvyy.particles

import OffsetsShader
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.debugOverlay
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.dvyy.particles.compute.*
import me.dvyy.particles.compute.GPUSort.gpuSorting
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.Meshes
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
//    val count: Int = (64 / 64) * 64
    val appScope = CoroutineScope(Dispatchers.RenderLoop)

    val application = startKoin {
        modules(module {
            single<CoroutineScope> { appScope }
            single<KoolContext> { ctx }
            singleOf(::ConfigRepository)
            singleOf(::UniformParameters)
            singleOf(::ParticleBuffers)
        }, module {
            singleOf(::CameraManager)
            singleOf(::ParticlesViewModel)
//            singleOf(::UniformsWindow)
            singleOf(::AppUI)
        })
    }
    val configRepo = application.koin.get<ConfigRepository>()
    val buffers = application.koin.get<ParticleBuffers>()
    val uniforms = application.koin.get<UniformParameters>()
    val config = configRepo.config.value

    val appScene = scene {
        // COMPUTE
        val sorting = ComputePass("Particles Compute")

        // Reset keys and indices based on grid cell particle is in
        val reset = GPUSort.resetBuffersShader.apply {
            uniform1f("gridSize", configRepo.gridSize)
            uniform1i("gridCols", configRepo.gridCells.x)
            uniform1i("gridRows", configRepo.gridCells.y)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("indices", buffers.sortIndices)
            storage1d("positions", buffers.positionBuffer)
        }
        sorting.addTask(reset, numGroups = Vec3i(configRepo.count / WORK_GROUP_SIZE, 1, 1))

        // Sort by grid cells
        gpuSorting(configRepo.count, buffers = buffers, computePass = sorting)

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

        val numGroups = Vec3i(configRepo.count / WORK_GROUP_SIZE, 1, 1)

        sorting.addTask(OffsetsShader.apply {
            uniform1i("numValues", configRepo.count)
            storage1d("keys", buffers.particleGridCellKeys)
            storage1d("offsets", buffers.offsetsBuffer)
        }, numGroups = numGroups)

        val boxMax = configRepo.boxSize
        val fields = FieldsShader(configRepo.config.value).also {
            it.gridSize = configRepo.gridSize
            it.gridCells = configRepo.gridCells
            it.count = configRepo.count
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

        repeat(config.simulation.passesPerFrame) { passIndex ->
            sorting.addTask(fields.halfStep, numGroups = numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        fields.halfStep_dT = simulation.dT.toFloat()
                    }
                }
            }
            sorting.addTask(fields.shader, numGroups = numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        fields.dT = simulation.dT.toFloat()
                        fields.maxVelocity = simulation.maxVelocity.toFloat()
                        fields.maxForce = simulation.maxForce.toFloat()
                    }
                    //TODO move up to whenDirty
                    uniforms.uniformParams.forEach { (param, value) ->
                        fields.shader.uniform1f(param.uniformName).set(value.value)
                    }
                }
            }
            addComputePass(sorting)
        }

        val convertShader = ConvertParticlesShader()
        sorting.addTask(convertShader.shader, numGroups = numGroups).apply {

            val conversionBuffer = Buffers.integers(config.particles.size)
            val conversionChances = Buffers.floats(config.particles.size)
            config.particles.forEachIndexed { id, from ->
                val to = from.convertTo
                if (to != null) {
                    conversionBuffer[id] = config.particleIds[to.type]!!.id.toInt()
                    conversionChances[id] = to.chance.toFloat()
                }
            }
            convertShader.convertTo = conversionBuffer
            convertShader.convertChances = conversionChances
            convertShader.particleTypes = buffers.particleTypesBuffer
            onBeforeDispatch {
                val shouldRun = Time.frameCount % 1000 == 0
                setNumGroupsByInvocations(if (shouldRun) configRepo.count else 0, 1, 1)
                convertShader.randomSeed = 1000f + (Time.gameTime % 1000.0).toFloat()
            }
        }

//         RENDERING

        application.koin.get<CameraManager>().manageCameraFor(this)
        val instances = Meshes.particleMeshInstances(configRepo.count)
        addNode(
            Meshes.particleMesh(
                buffers,
                instances
            )
        )
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
                val count = configRepo.count
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
