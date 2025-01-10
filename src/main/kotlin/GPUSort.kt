import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

object GPUSort {
    val computeShader = ComputeShader.fromCode(File("data/compute-shaders/sorter.comp").readText(), "sorter")
    val offsetsShader = ComputeShader.fromCode(File("data/compute-shaders/offsets.comp").readText(), "offsets")

    fun sort(
        keys: VertexBuffer,
        values: VertexBuffer,
    ) {
        computeShader.apply {
            uniform("numValues", values.vertexCount)
            buffer("keysBuffer", keys)
            buffer("valuesBuffer", values)
        }

        val numPairs = Integer.highestOneBit(values.vertexCount) * 2
        val numStages = Integer.numberOfTrailingZeros(numPairs)

        for (stageIndex in 0..<numStages) {
            for (stepIndex in 0..stageIndex) {
                val groupWidth = 1 shl (stageIndex - stepIndex)
                val groupHeight = 2 * groupWidth - 1
//                println("stageIndex: $stageIndex, stepIndex: $stepIndex")
//                println("groupWidth: $groupWidth, groupHeight: $groupHeight")

                computeShader.apply {
                    uniform("groupWidth", groupWidth)
                    uniform("groupHeight", groupHeight)
                    uniform("stepIndex", stepIndex)
                    computeShader.execute(numPairs)
                }
            }
        }
    }

    fun calculateOffsets(
        keys: VertexBuffer,
        offsets: VertexBuffer,
        numValues: Int,
    ) {
        offsetsShader.apply {
            uniform("numValues", numValues)
            buffer("keysBuffer", keys)
            buffer("offsetsBuffer", offsets)
        }
        offsetsShader.execute(numValues)
    }
}
