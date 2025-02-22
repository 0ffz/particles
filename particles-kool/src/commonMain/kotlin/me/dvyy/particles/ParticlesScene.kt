package me.dvyy.particles

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.launchOnMainThread
import de.fabmax.kool.util.releaseWith
import me.dvyy.particles.compute.*
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.render.CameraManager
import me.dvyy.particles.render.ParticlesMesh
import offsetsShader

class ParticlesScene(
    val buffers: ParticleBuffers,
    val uniforms: UniformParameters,
    val configRepo: ConfigRepository,
    val gpuSort: GPUSort,
    val cameraManager: CameraManager,
    val particlesMesh: ParticlesMesh,
) {
    val config = configRepo.config.value

    val scene = scene {
        buffers.releaseWith(this)
        onRelease {
            println("Released $this")
        }
        // COMPUTE
        val computePass = ComputePass("Particles Compute")

        // Reset keys and indices based on grid cell particle is in
        gpuSort.addResetShader(computePass)
        // Sort by grid cells
        gpuSort.addSortingShader(configRepo.count, buffers = buffers, computePass = computePass)

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

        computePass.addTask(offsetsShader().apply {
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
            computePass.addTask(fields.halfStep, numGroups = numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        fields.halfStep_dT = simulation.dT.toFloat()
                        val count = simulation.targetCount
                        setNumGroupsByInvocations(count)
                    }
                }
            }
            computePass.addTask(fields.shader, numGroups = numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        fields.dT = simulation.dT.toFloat()
                        fields.maxVelocity = simulation.maxVelocity.toFloat()
                        fields.maxForce = simulation.maxForce.toFloat()
                        val count = simulation.targetCount
                        setNumGroupsByInvocations(count)
                        fields.count = count
                    }
                    //TODO move up to whenDirty
                    uniforms.uniformParams.forEach { (param, value) ->
                        fields.shader.uniform1f(param.uniformName).set(value.value)
                    }
                }
            }
        }

        val convertShader = ConvertParticlesShader()
        computePass.addTask(convertShader.shader, numGroups = numGroups).apply {

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
                val count = configRepo.count
                val shouldRun = Time.frameCount % configRepo.config.value.simulation.conversionRate == 0 //TODO make configurable, real world seconds
                setNumGroupsByInvocations(if (shouldRun) configRepo.count else 0, 1, 1)
                convertShader.randomSeed = count + (Time.gameTime % count).toFloat()
            }
        }
//         RENDERING

        cameraManager.manageCameraFor(this)
        addNode(particlesMesh.mesh)
        addComputePass(computePass)

        var iterations = 0
        onUpdate {
            iterations++
            return@onUpdate
            if (iterations % 90 * 5 == 0) launchOnMainThread {
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
}
