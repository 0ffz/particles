package shaders

import helpers.Helpers
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File
import kotlin.io.path.Path

class UpdateIndices(
    val numValues: Int,
    gridSize: Double,
    gridCols: Int,
    val keys: VertexBuffer,
//    indices: VertexBuffer,
) {
    val computeShader = Helpers.computeShader(Path("data/compute-shaders/updateIndices.comp"), "updateIndices").apply {
        uniform("gridSize", gridSize)
        uniform("gridCols", gridCols)
//        buffer("keysBuffer", keys)
//        buffer("indicesBuffer", indices)
    }

    fun run(positions: VertexBuffer) {
        computeShader.apply {
            buffer("positionsBuffer", positions)
            buffer("keysBuffer", keys)
        }
        computeShader.execute(numValues / 32)
    }
}
