package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.scene.Scene
import me.dvyy.particles.FieldsBuffers

const val WORK_GROUP_SIZE = 64

//val ReindexShader = KslComputeShader("Reindex Elements") {
//    computeStage(WORK_GROUP_SIZE) {
//        val indices = storage1d<KslInt1>("indices")
//
//    }
//}
object GPUSort {
    /**
     * Given particles positions and grid info, resents keys and indices buffers such that:
     * - Keys point to the grid cell of particle at index
     * - Indices are sorted 1 to count
     */
    val resetBuffersShader = KslComputeShader("ResetBuffers") {
        computeStage(WORK_GROUP_SIZE) {
            val gridSize = uniformFloat1("gridSize")
            val gridRows = uniformInt1("gridRows")
            val gridCols = uniformInt1("gridCols")
            val keys = storage1d<KslInt1>("keys")
            val indices = storage1d<KslInt1>("indices")
            val positions = storage1d<KslFloat4>("positions")

            // Helper: compute cell id from grid coordinates (cell id = x + y * gridCols)
            val cellId = functionInt1("cellId") {
                val xGrid = paramInt1("xGrid")
                val yGrid = paramInt1("yGrid")
                val zGrid = paramInt1("zGrid")
//                val gridCols = paramInt1("gridCols")
                body {
                    xGrid + (yGrid * gridCols) + (zGrid * gridRows * gridCols)
                }
            }
            main {
                // get global invocation id
                val idx = int1Var(inGlobalInvocationId.x.toInt1())
                val position = positions[idx]
                val xGrid = int1Var((position.x / gridSize).toInt1());
                val yGrid = int1Var((position.y / gridSize).toInt1());
                val zGrid = int1Var((position.z / gridSize).toInt1());
                val cellId = int1Var(cellId(xGrid, yGrid, zGrid))
                keys[idx] = cellId
                indices[idx] = idx
            }
        }
    }

    val shader = KslComputeShader("GPUSort") {
        computeStage(WORK_GROUP_SIZE) {
            val numValues = uniformInt1("numValues")
            val groupWidth = uniformInt1("groupWidth")
            val groupHeight = uniformInt1("groupHeight")
            val stepIndex = uniformInt1("stepIndex")

            val keys = storage1d<KslInt1>("keys")
            val indices = storage1d<KslInt1>("indices")
            val currPositions = storage1d<KslFloat4>("currPositions")
            val currVelocities = storage1d<KslFloat4>("currVelocities")
//            val prevPositions = storage1d<KslFloat4>("prevPositions")
//            val prevVelocities = storage1d<KslFloat4>("prevVelocities")
            val prevForces = storage1d<KslFloat4>("prevForces")
            val types = storage1d<KslInt1>("types")

            main {
                val i = int1Var(inGlobalInvocationId.x.toInt1())
                val h = int1Var(i and (groupWidth - 1.const))
                val indexLow = int1Var(h + (groupHeight + 1.const) * (i / groupWidth))
                val indexHigh = int1Var(0.const)
                `if`(stepIndex eq 0.const) {
                    indexHigh set indexLow + (groupHeight - 2.const * h)
                }.`else` {
                    indexHigh set indexLow + (groupHeight + 1.const) / 2.const
                }

                `if`(indexHigh lt numValues) {
                    val keyLow = int1Var(keys[indexLow])
                    val keyHigh = int1Var(keys[indexHigh])

                    `if`(keyLow gt keyHigh) {
                        fun swapFloats(buffer: KslStorage1d<KslStorage1dType<KslFloat4>>) {
                            val currLow = float4Var(buffer[indexLow])
                            val currHigh = float4Var(buffer[indexHigh])
                            buffer[indexLow] = currHigh
                            buffer[indexHigh] = currLow
                        }
                        fun swapInts(buffer: KslStorage1d<KslStorage1dType<KslInt1>>) {
                            val currLow = int1Var(buffer[indexLow])
                            val currHigh = int1Var(buffer[indexHigh])
                            buffer[indexLow] = currHigh
                            buffer[indexHigh] = currLow
                        }
                        swapFloats(currPositions)
                        swapFloats(currVelocities)
//                        swapFloats(prevPositions)
//                        swapFloats(prevVelocities)
                        swapFloats(prevForces)
                        swapInts(types)
                        swapInts(keys)
                        swapInts(indices)
                    }
                }
            }
        }
    }

    fun Scene.gpuSorting(
        count: Int,
        buffers: FieldsBuffers,
        computePass: ComputePass,
    ) {
        val sorter = shader
        var keys by sorter.storage1d("keys")
        var indices by sorter.storage1d("indices")
        var numValues by sorter.uniform1i("numValues")
        var groupWidthU by sorter.uniform1i("groupWidth")
        var groupHeightU by sorter.uniform1i("groupHeight")
        var stepIndexU by sorter.uniform1i("stepIndex")
        var positions1 by sorter.storage1d("currPositions")
        var velocities1 by sorter.storage1d("currVelocities")
        var prevForces by sorter.storage1d("prevForces")
        var types by sorter.storage1d("types")

        numValues = count
        keys = buffers.particleGridCellKeys
        indices = buffers.sortIndices
        positions1 = buffers.positionBuffer
        velocities1 = buffers.velocitiesBuffer
        prevForces = buffers.forcesBuffer
        types = buffers.particleTypesBuffer

        computePass.apply {
            val numPairs = count.takeHighestOneBit() * 2
            val numStages = numPairs.countTrailingZeroBits()
            for (stageIndex in 0..<numStages) {
                for (stepIndex in 0..stageIndex) {
                    val groupWidth = 1 shl (stageIndex - stepIndex)
                    val groupHeight = 2 * groupWidth - 1
                    addTask(sorter, numGroups = Vec3i(numPairs / WORK_GROUP_SIZE, 1, 1)).apply {
                        pipeline.swapPipelineData("$stageIndex, $stepIndex")
                        groupWidthU = groupWidth
                        groupHeightU = groupHeight
                        stepIndexU = stepIndex
                        onBeforeDispatch {
                            pipeline.swapPipelineData("$stageIndex, $stepIndex")
                        }
                    }
                }
            }
        }
//        keysBuffer.releaseWith(this)
//        indicesBuffer.releaseWith(this)
    }
}
