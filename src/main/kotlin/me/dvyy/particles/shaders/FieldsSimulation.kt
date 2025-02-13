package me.dvyy.particles.shaders

import me.dvyy.particles.YamlParameters
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.helpers.Helpers
import org.openrndr.draw.VertexBuffer
import kotlin.io.path.Path

class FieldsSimulation(
    val gridSize: Double,
    val gridRows: Int,
    val gridCols: Int,
    val count: Int,
    val computeWidth: Int,
    val particle2CellKey: VertexBuffer,
    val colorBuffer: VertexBuffer,
    val parameters: YamlParameters,
    config: ParticlesConfig,
) {
    val fieldsShader = Helpers.computeShader(
        Path("/data/compute-shaders/fields.comp"),
        "fields",
        templates = mapOf(
            "uniforms" to config.pairwiseInteraction.joinToString("\n") { interaction ->
                interaction.uniformStrings()
            },
            "forceFunctions" to config.functions.joinToString(separator = "\n") { it.render() },
            "forceCalculations" to config.pairwiseInteraction.joinToString(separator = "\n") { interaction ->
                """
                case 0x${interaction.type.hash}: {
                    forceBetweenParticles += ${
                    interaction.functions.joinToString(separator = " + ") {
                        it.renderFunctionCall()
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
        parameters.dirtyUniforms.forEach {
            uniform(it.uniform.uniformName, (it.value as String).toDouble())
        }
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
