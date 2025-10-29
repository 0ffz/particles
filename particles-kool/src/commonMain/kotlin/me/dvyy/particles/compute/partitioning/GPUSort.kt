package me.dvyy.particles.compute.partitioning

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.helpers.cellId
import me.dvyy.particles.config.ConfigRepository

const val WORK_GROUP_SIZE = 64

class ResetBuffers(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
) {

    /**
     * Given particles positions and grid info, resets keys and indices buffers such that:
     * - Keys point to the grid cell of particle at index
     * - Indices are sorted 1 to count
     */
    val resetBuffersShader = KslComputeShader("ResetBuffers") {
        computeStage(WORK_GROUP_SIZE) {
            val gridSize = uniformFloat1("gridSize")
            val gridCells = uniformInt3("gridCells")
            val cellIdKeys = storage<KslInt1>("keys")
            val indices = storage<KslInt1>("indices")
            val positions = storage<KslFloat4>("positions")

            // Helper: compute cell id from grid coordinates (cell id = x + y * gridCols)


            main {
                // get global invocation id
                val idx = int1Var(inGlobalInvocationId.x.toInt1())
                val position = positions[idx].xyz
                val grid = int3Var((position / gridSize).toInt3())
//                val xGrid = int1Var((position.x / gridSize).toInt1())
//                val yGrid = int1Var((position.y / gridSize).toInt1())
//                val zGrid = int1Var((position.z / gridSize).toInt1())
                val cellId = int1Var(cellId(grid, gridCells))
                cellIdKeys[idx] = cellId
                indices[idx] = idx
            }
        }
    }

    fun addResetShader(
        computePass: ComputePass,
    ) {
        val reset = resetBuffersShader.apply {
            uniform1f("gridSize", configRepo.gridSize)
            uniform3i("gridCells", configRepo.gridCells)
            storage("keys", buffers.particleGridCellKeys)
            storage("indices", buffers.sortIndices)
            storage("positions", buffers.positionBuffer)
        }
        computePass.addTask(reset, numGroups = configRepo.numGroups)
    }
}

class GPUSort {
    val sorter = KslComputeShader("GPUSort") {
        computeStage(WORK_GROUP_SIZE) {
            val numValues = uniformInt1("numValues")
            val groupWidth = uniformInt1("groupWidth")
            val groupHeight = uniformInt1("groupHeight")
            val stepIndex = uniformInt1("stepIndex")

            val cellIdKeys = storage<KslInt1>("keys")
            val indices = storage<KslInt1>("indices")

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
                    val keyLow = int1Var(cellIdKeys[indexLow])
                    val keyHigh = int1Var(cellIdKeys[indexHigh])

                    `if`(keyLow gt keyHigh) {
                        fun swapFloats(buffer: KslPrimitiveStorage<KslPrimitiveStorageType<KslFloat4>>) {
                            val currLow = float4Var(buffer[indexLow])
                            val currHigh = float4Var(buffer[indexHigh])
                            buffer[indexLow] = currHigh
                            buffer[indexHigh] = currLow
                        }

                        fun swapInts(buffer: KslPrimitiveStorage<KslPrimitiveStorageType<KslInt1>>) {
                            val currLow = int1Var(buffer[indexLow])
                            val currHigh = int1Var(buffer[indexHigh])
                            buffer[indexLow] = currHigh
                            buffer[indexHigh] = currLow
                        }
                        swapInts(cellIdKeys)
                        swapInts(indices)
                    }
                }
            }
        }
    }

    var keys by sorter.storage("keys")
    var indices by sorter.storage("indices")
    var numValues by sorter.uniform1i("numValues")
    var groupWidthU by sorter.uniform1i("groupWidth")
    var groupHeightU by sorter.uniform1i("groupHeight")
    var stepIndexU by sorter.uniform1i("stepIndex")

    fun addSortingShader(
        count: Int,
        buffers: ParticleBuffers,
        computePass: ComputePass,
    ) {
        numValues = count
        keys = buffers.particleGridCellKeys
        indices = buffers.sortIndices

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
    }
}
