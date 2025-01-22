package me.dvyy.particles.shaders

import me.dvyy.particles.helpers.Helpers
import org.openrndr.draw.VertexBuffer
import kotlin.io.path.Path

class GPUSort(
    val numValues: Int,
//    val indices: VertexBuffer,
    val sortByKey: VertexBuffer,
    val offsets: VertexBuffer,
    val types: VertexBuffer,
) {
    val sorterShader = Helpers.computeShader(Path("/data/compute-shaders/sorter.comp"), "sorter").apply {
        uniform("numValues", numValues)
//        buffer("keysBuffer", sortByKey)
//        buffer("valuesBuffer", indices)
    }

    val offsetsShader = Helpers.computeShader(Path("/data/compute-shaders/offsets.comp"), "offsets").apply {
        uniform("numValues", numValues)
//        buffer("keysBuffer", sortByKey)
//        buffer("offsetsBuffer", offsets)
    }

    fun sort(
//        sortByKey: VertexBuffer,
        values: VertexBuffer,
        prevValues: VertexBuffer,
    ) {
        sorterShader.run {
            buffer("keysBuffer", sortByKey)
            buffer("valuesBuffer", values)
            buffer("prevValuesBuffer", prevValues)
            buffer("typesBuffer", types)
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

    fun calculateOffsets() = offsetsShader.run {
        buffer("keysBuffer", sortByKey)
        buffer("offsetsBuffer", offsets)
        execute(numValues / 32)
    }
}
