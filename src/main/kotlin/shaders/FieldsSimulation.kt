package shaders

import SimulationConstants
import SimulationSettings
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import java.io.File

class FieldsSimulation(
    val gridSize: Double,
    val gridRows: Int,
    val gridCols: Int,
    val settings: SimulationSettings,
    val count: Int,
    val computeWidth: Int,
    val particle2CellKey: VertexBuffer,
    val cellOffsets: VertexBuffer,
    val colorBuffer: VertexBuffer,
) {
    val fieldsShader = ComputeShader.fromCode(File("data/compute-shaders/fields.comp").readText(), "fields").apply {
        uniform("gridSize", gridSize)
        uniform("gridRows", gridRows)
        uniform("gridCols", gridCols)
        uniform("count", count)
        uniform("sigma", SimulationConstants.sigma)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
    }

    fun run(
        cellOffsets: VertexBuffer,
        currPositions: VertexBuffer,
        prevPositions: VertexBuffer,
    ) = fieldsShader.apply {
        uniform("epsilon", settings.epsilon)
        uniform("dT", SimulationSettings.deltaT)
        uniform("maxForce", SimulationSettings.maxForce)
        uniform("maxVelocity", SimulationSettings.maxVelocity)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
        buffer("cellOffsetsBuffer", cellOffsets)
        buffer("particle2CellKeyBuffer", particle2CellKey)
        buffer("currParticlesBuffer", currPositions)
        buffer("prevParticlesBuffer", prevPositions)
        buffer("colorBuffer", colorBuffer)
        fieldsShader.execute(computeWidth)
    }
}
