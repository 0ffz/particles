package me.dvyy.particles.shaders

import me.dvyy.particles.SimulationConstants
import me.dvyy.particles.SimulationSettings
import me.dvyy.particles.dsl.ParticlesConfiguration
import me.dvyy.particles.helpers.Helpers
import org.openrndr.draw.VertexBuffer
import kotlin.io.path.Path

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
    config: ParticlesConfiguration,
) {
    val fieldsShader = Helpers.computeShader(
        Path("/data/compute-shaders/fields.comp"),
        "fields",
        templates = mapOf(
            "forceFunctions" to config.functions.joinToString(separator = "\n") { it.render() },
            "forceCalculations" to config.interactions.joinToString(separator = "\n") { interaction ->
                """
                case 0x${interaction.hash}: {
                    forceBetweenParticles += ${
                    interaction.functions.joinToString(separator = " + ") {
                        "${it.function.name}(dist${it.getParameters().joinToString(prefix = ", ", separator = ", ")})"
                    }
                };
                    break;
                }
                """.trimIndent()
            }
        )
    ).apply {
        uniform("gridSize", gridSize)
        uniform("gridRows", gridRows)
        uniform("gridCols", gridCols)
        uniform("count", count)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
    }

    fun run(
        cellOffsets: VertexBuffer,
        currPositions: VertexBuffer,
        prevPositions: VertexBuffer,
        particleTypes: VertexBuffer,
    ) = fieldsShader.apply {
        uniform("dT", SimulationSettings.deltaT)
        uniform("maxForce", SimulationSettings.maxForce)
        uniform("maxVelocity", SimulationSettings.maxVelocity)
//        buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
        buffer("cellOffsetsBuffer", cellOffsets)
        buffer("particle2CellKeyBuffer", particle2CellKey)
        buffer("currParticlesBuffer", currPositions)
        buffer("prevParticlesBuffer", prevPositions)
        buffer("colorBuffer", colorBuffer)
        buffer("particleTypesBuffer", particleTypes)
        fieldsShader.execute(computeWidth)
    }
}
