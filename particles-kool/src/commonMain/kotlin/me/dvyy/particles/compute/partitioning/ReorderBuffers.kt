package me.dvyy.particles.compute.partitioning

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.pipeline.GpuBuffer

/**
 * Given the resulting positions of an indices array that was 0..n before sorting,
 * moves other buffers as though they were sorted alongside the indices.
 *
 * Ex. Suppose we had the following indices:
 * - `[0,1,2,3]` before sorting
 * - `[3,0,1,2]` after sorting
 *
 * Then this shader moves other buffers of size 4 such that elements move as follows:
 * ```
 * 0 -> 3
 * 1 -> 0
 * 2 -> 1
 * 3 -> 2
 * ```
 */
class ReorderBuffersShader(
    private val buffersToSort: List<GpuBuffer>,
//    private val outputBuffer: GpuBuffer,
) {
    private val outputs = buffersToSort.map { buffer ->
        GpuBuffer(buffer.type, buffer.usage, buffer.size)
    }

    val shader = KslComputeShader("Reorder buffers") {
        val numValues = uniformInt1("numValues")
        val postSortIndices = storage<KslInt1>("indices")
        val storageToSort = buffersToSort.mapIndexed { id, _ -> storage<KslNumericType>("storage_$id") }
        val outputs = buffersToSort.mapIndexed { id, _ -> storage<KslNumericType>("output_$id") }

        computeStage(WORK_GROUP_SIZE) {
            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val destId = int1Var(postSortIndices[id])
                `if`((id lt numValues)) {
                    storageToSort.zip(outputs).forEach { (input, output) ->
                        output[id] = input[destId]
                    }
                }
            }
        }
    }

    val copyBack = KslComputeShader("Copy back") {
        val numValues = uniformInt1("numValues")
        val inputs = buffersToSort.mapIndexed { id, _ -> storage<KslNumericType>("storage_$id") }
        val outputs = buffersToSort.mapIndexed { id, _ -> storage<KslNumericType>("output_$id") }
        computeStage(WORK_GROUP_SIZE) {
            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                `if`((id lt numValues)) {
                    inputs.zip(outputs).forEach { (input, output) ->
                        input[id] = output[id]
                    }
                }
            }
        }
    }

//    var output by shader.storage("outputBuffer")

    fun bindStorage() {
    }

    fun addTo(
        stage: ComputePass,
        indices: GpuBuffer,
        numValues: Int,
        numGroups: Vec3i
    ) = stage.apply {
        shader.uniform1i("numValues", numValues)
        shader.storage("indices", indices)
        copyBack.uniform1i("numValues", numValues)
        buffersToSort.forEachIndexed { id, buffer ->
            shader.storage("storage_$id", buffer)
            shader.storage("output_$id", outputs[id])
            copyBack.storage("storage_$id", buffer)
            copyBack.storage("output_$id", outputs[id])
        }
        stage.addTask(shader, numGroups)
        stage.addTask(copyBack, numGroups)
    }
}
