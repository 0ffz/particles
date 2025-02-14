package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.pipeline.StorageBuffer1d
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.launchOnMainThread
import de.fabmax.kool.util.releaseWith

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
            val gridCols = uniformInt1("gridSize")
            val keys = storage1d<KslInt1>("keys")
            val indices = storage1d<KslInt1>("indices")
            val positions = storage1d<KslFloat4>("positions")

            val cellId = functionInt1("cellId") {
                val xGrid = paramInt1("xGrid")
                val yGrid = paramInt1("yGrid")
                body {
                    xGrid + (yGrid * gridCols)
                }
            }
            main {
                // get global invocation id
                val idx = int1Var(inGlobalInvocationId.x.toInt1())
                val position = positions[idx]
                val xGrid = int1Var((position.x / gridSize).toInt1());
                val yGrid = int1Var((position.y / gridSize).toInt1());
                val cellId = int1Var(cellId(xGrid, yGrid))
                keys[idx] = cellId
                indices[idx] = idx
            }
        }
    }
    val shader = KslComputeShader("GPUSort") {
        computeStage(WORK_GROUP_SIZE) {
//        val groupWidth = uniformInt1("groupWidth")
//        val groupHeight = uniformInt1("groupHeight")
//        val stepIndex = uniformInt1("stepIndex")

            val keys = storage1d<KslInt1>("keys")
            val indices = storage1d<KslInt1>("indices")

            val numValues = uniformInt1("numValues")
            val stage = uniformInt1("stage")
            val passOfStage = uniformInt1("passOfStage")

            main {
                // get global invocation id
                val idx = int1Var(inGlobalInvocationId.x.toInt1())

                // pairDistance = 1 << (stage - passOfStage)
                val diff = int1Var(stage - passOfStage)
                val pairDistance = int1Var(1.const shl diff)

                // blockWidth = 2 * pairDistance
                val blockWidth = int1Var(2.const * pairDistance)

                // leftId = (idx / pairDistance) * blockWidth + (idx rem pairDistance)
                val leftId = int1Var((idx / pairDistance) * blockWidth + idx.rem(pairDistance))
                // rightId = leftId + pairDistance
                val rightId = int1Var(leftId + pairDistance)

                `if`(rightId lt numValues) {
                    // Determine sorting direction: ascending if ((idx / (1 << stage)) & 1) == 0
                    val temp = int1Var(1.const shl stage)
                    val ascending = bool1Var(((idx / temp) and 1.const) eq 0.const)

                    // Load the two elements from the buffer
                    val leftIndex = int1Var(indices[leftId])
                    val rightIndex = int1Var(indices[rightId])
                    val leftKey = int1Var(keys[leftId])
                    val rightKey = int1Var(keys[rightId])

                    // Decide whether to swap based on the comparison and the sort order
                    val needSwap = bool1Var((leftKey gt rightKey) eq ascending)
                    `if`(needSwap) {
                        indices[leftId] = rightIndex
                        indices[rightId] = leftIndex
                        keys[leftId] = rightKey
                        keys[rightId] = leftKey
                    }
                }
            }
        }
    }

    fun Scene.gpuSorting(
        count: Int,
        keysBuffer: StorageBuffer1d,
        indicesBuffer: StorageBuffer1d,
    ) {
        val sorter = shader
        var keys by sorter.storage1d("keys")
        var indices by sorter.storage1d("indices")
        var numValues by sorter.uniform1i("numValues")
        var stage by sorter.uniform1i("stage")
        var passOfStage by sorter.uniform1i("passOfStage")

        keysBuffer.releaseWith(this)
        indicesBuffer.releaseWith(this)
//
        numValues = count
        keys = keysBuffer
        indices = indicesBuffer
//
        val pass = ComputePass("Sorting pass").apply {
            val numPairs = count.takeHighestOneBit() * 2
            val numStages = numPairs.countTrailingZeroBits()
            for (stageIndex in 1..numStages) {
                for (stepIndex in 0 until stageIndex) {
                    addTask(sorter, numGroups = Vec3i(count / WORK_GROUP_SIZE)).onBeforeDispatch {
                        stage = stageIndex
                        passOfStage = stepIndex
                    }
                }
            }
        }
        addComputePass(pass)
//
        launchOnMainThread {
            keysBuffer.readbackBuffer()
            indicesBuffer.readbackBuffer()
            println((0 until count).map { keysBuffer.getI1(it) }.toString())
            println((0 until count).map { indicesBuffer.getI1(it) }.toString())
        }
    }
}
