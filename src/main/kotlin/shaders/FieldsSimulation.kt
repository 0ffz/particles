package shaders

import SimulationSettings
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

class FieldsSimulation(
    val gridSize: Int,
    val gridRows: Int,
    val gridCols: Int,
    val settings: SimulationSettings,
    val deltaT: Double,
    val count: Int,
    val computeWidth: Int,
    val computeHeight: Int,
//    val sortedParticleIndices: VertexBuffer,
    val particle2CellKey: VertexBuffer,
    val cellOffsets: VertexBuffer,
    val colorBuffer: VertexBuffer,
) {
    val fieldsShader = ComputeShader.fromCode(File("data/compute-shaders/fields.comp").readText(), "fields").apply {
        uniform("gridSize", gridSize)
        uniform("gridRows", gridRows)
        uniform("gridCols", gridCols)
        uniform("dT", deltaT)
        uniform("count", count)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
        buffer("particle2CellKeyBuffer", particle2CellKey)
        buffer("cellStartIndicesBuffer", cellOffsets)
    }

    fun run(
        cellOffsets: VertexBuffer,
        currPositions: VertexBuffer,
        prevPositions: VertexBuffer,
    ) = fieldsShader.apply {
        uniform("epsilon", settings.epsilon)
        uniform("sigma", SimulationConstants.sigma)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
        buffer("cellStartIndicesBuffer", cellOffsets)
        buffer("currParticlesBuffer", currPositions)
        buffer("prevParticlesBuffer", prevPositions)
        buffer("colorBuffer", colorBuffer)
        fieldsShader.execute(computeWidth, computeHeight)
    }
}
