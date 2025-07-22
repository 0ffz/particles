package me.dvyy.particles.compute.partitioning

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.ParticleStruct
import me.dvyy.particles.compute.helpers.cellId
import me.dvyy.particles.config.ConfigRepository

const val WORK_GROUP_SIZE = 64

class GPUSort(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
) {
    /**
     * Given particles positions and grid info, resets keys and indices buffers such that:
     * - Keys point to the grid cell of particle at index
     * - Indices are sorted 1 to count
     */
    val resetBuffersShader = KslComputeShader("ResetBuffers") {
        val particleStruct = struct { ParticleStruct() }
        computeStage(WORK_GROUP_SIZE) {
            val gridSize = uniformFloat1("gridSize")
            val gridCells = uniformInt3("gridCells")
            val particles = storage("particles", particleStruct)

            // Helper: compute cell id from grid coordinates (cell id = x + y * gridCols)

            val cellId = cellId(gridCells)

            main {
                // get global invocation id
                val idx = int1Var(inGlobalInvocationId.x.toInt1())
                val particleVar = structVar(particles[idx])
                val particle = particleVar.struct
//                val grid = int3Var((particle.position.ksl / gridSize).toInt3())
//                val cellId = int1Var(cellId(grid.x, grid.y, grid.z))
                particle.gridCellId.ksl set particle.gridCellId.ksl + 1.const
                particles[idx] = particleVar
            }
        }
    }

    fun addResetShader(
        computePass: ComputePass
    ) {
        val reset = resetBuffersShader.apply {
            uniform1f("gridSize", configRepo.gridSize)
            uniform3i("gridCells", configRepo.gridCells)
            storage("particles", buffers.particleBuffer)
        }
        computePass.addTask(reset, numGroups = configRepo.numGroups)
    }

    val sorter = KslComputeShader("GPUSort") {
        val particleStruct = struct { ParticleStruct() }
        computeStage(WORK_GROUP_SIZE) {
            val numValues = uniformInt1("numValues")
            val groupWidth = uniformInt1("groupWidth")
            val groupHeight = uniformInt1("groupHeight")
            val stepIndex = uniformInt1("stepIndex")

            val particles = storage("particles", particleStruct)
//            val cellIdKeys = storage<KslInt1>("keys")
//            val positions = storage<KslFloat4>("currPositions")
//            val velocities = storage<KslFloat4>("currVelocities")
//            val forces = storage<KslFloat4>("prevForces")
//            val types = storage<KslInt1>("types")
//            val clusters = storage<KslInt1>("clusters")

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
                    //TODO separate storage for indices?
                    val keyLow = int1Var(particles[indexLow].struct.gridCellId.ksl)
                    val keyHigh = int1Var(particles[indexHigh].struct.gridCellId.ksl)

                    `if`(keyLow gt keyHigh) {
                        val currLow = structVar(particles[indexLow])
                        val currHigh = structVar(particles[indexHigh])
                        particles[indexLow] = currHigh
                        particles[indexHigh] = currLow
                    }
                }
            }
        }
    }

    var numValues by sorter.uniform1i("numValues")
    var groupWidthU by sorter.uniform1i("groupWidth")
    var groupHeightU by sorter.uniform1i("groupHeight")
    var stepIndexU by sorter.uniform1i("stepIndex")
    var particles by sorter.storage("particles")

    fun addSortingShader(
        count: Int,
        computePass: ComputePass,
    ) {
        numValues = count
        particles = buffers.particleBuffer

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
