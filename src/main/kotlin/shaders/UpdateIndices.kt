package shaders

import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

class UpdateIndices(
    val numValues: Int,
    gridSize: Int,
    gridCols: Int,
    keys: VertexBuffer,
//    indices: VertexBuffer,
) {
    val computeShader = ComputeShader.fromCode(File("data/compute-shaders/updateIndices.comp").readText(), "updateIndices").apply {
        uniform("gridSize", gridSize)
        uniform("gridCols", gridCols)
        buffer("keysBuffer", keys)
//        buffer("indicesBuffer", indices)
    }

    fun run(positions: VertexBuffer) {
        computeShader.apply {
            buffer("positionsBuffer", positions)
        }
        computeShader.execute(numValues)
    }
}
