import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

object UpdateIndices {
    val computeShader = ComputeShader.fromCode(File("data/compute-shaders/updateIndices.comp").readText(), "updateIndices")

    fun updateIndices(
        gridSize: Int,
        gridCols: Int,
        keys: VertexBuffer,
        indices: VertexBuffer,
        positions: VertexBuffer,
    ) {
        computeShader.apply {
            uniform("gridSize", gridSize)
            uniform("gridCols", gridCols)
            buffer("keysBuffer", keys)
            buffer("indicesBuffer", indices)
            buffer("positionsBuffer", positions)
        }

        computeShader.execute(indices.vertexCount)
    }
}
