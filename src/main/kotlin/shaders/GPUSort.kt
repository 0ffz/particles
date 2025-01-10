package shaders

import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

class GPUSort(
    val numValues: Int,
    val indices: VertexBuffer,
    val offsets: VertexBuffer,
) {
    val sorterShader = ComputeShader.fromCode(File("data/compute-shaders/sorter.comp").readText(), "sorter").apply {
        uniform("numValues", numValues)
//        buffer("valuesBuffer", indices)
    }

    val offsetsShader = ComputeShader.fromCode(File("data/compute-shaders/offsets.comp").readText(), "offsets").apply {
        uniform("numValues", numValues)
//        buffer("offsetsBuffer", offsets)
    }

    fun sort(
        keys: VertexBuffer,
    ) {
        sorterShader.run {
            buffer("keysBuffer", keys)
            buffer("valuesBuffer", indices)
        }

        val numPairs = Integer.highestOneBit(numValues) * 2
        val numStages = Integer.numberOfTrailingZeros(numPairs)

        for (stageIndex in 0..<numStages) {
            for (stepIndex in 0..stageIndex) {
                val groupWidth = 1 shl (stageIndex - stepIndex)
                val groupHeight = 2 * groupWidth - 1

                sorterShader.run {
                    uniform("groupWidth", groupWidth)
                    uniform("groupHeight", groupHeight)
                    uniform("stepIndex", stepIndex)
                    execute(numPairs)
                }
            }
        }
    }

    fun calculateOffsets(
        keys: VertexBuffer,
    ) = offsetsShader.run {
        buffer("keysBuffer", keys)
        buffer("offsetsBuffer", offsets)
        execute(numValues)
    }
}
