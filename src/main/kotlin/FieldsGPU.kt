import extensions.FPSDisplay
import helpers.Buffers
import helpers.Drawing.offsetGeometry
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import shaders.FieldsSimulation
import shaders.GPUSort
import shaders.UpdateIndices
import kotlin.math.sqrt

fun main() = application {
    val gridSize = (2.5 * sigma).toInt()
    val computeWidth = sqrt(targetCount / 32.0).toInt()
    val computeHeight = computeWidth
    val count = computeWidth * computeHeight * 32

    configure {
//        width = 1000
//        height = 1000
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        windowResizable = true
    }

    program {
        println(width)
        val area = drawer.bounds//.offsetEdges(-10.0)
        val gridCols = width / gridSize
        val gridRows = height / gridSize

        // Initialize buffers
        val positionBuffers = List(2) { listId ->
            vertexBuffer(
                vertexFormat { position(2) },
                count,
            ).apply {
                Random.resetState()
                put {
                    repeat(vertexCount) {
                        write(Random.point(area))
                    }
                }
            }
        }
        val particle2CellKey = Buffers.uInt(count)
//        val sortedParticleIndices = Buffers.uInt(count)
        val cellOffsets = Buffers.uInt(count)
        val colorBuffer = Buffers.colorBuffer(ColorRGBa.BLACK, count)
        val geometry = Buffers.circleFanGeometry()

        // Initialize compute shaders
        val updateIndices = UpdateIndices(
            numValues = count,
            gridSize = gridSize,
            gridCols = gridCols,
            keys = particle2CellKey,
//            indices = sortedParticleIndices,
        )

        val fieldsSimulation = FieldsSimulation(
            gridSize = gridSize,
            gridRows = gridRows,
            gridCols = gridCols,
            epsilon = epsilon,
            sigma = sigma,
            deltaT = deltaT,
            count = count,
            computeWidth = computeWidth,
            computeHeight = computeHeight,
//            sortedParticleIndices = sortedParticleIndices,
            particle2CellKey = particle2CellKey,
            cellOffsets = cellOffsets,
            colorBuffer = colorBuffer,
        )

        val gpuSort = GPUSort(
            numValues = count,
//            indices = sortedParticleIndices,
            sortByKey = particle2CellKey,
            offsets = cellOffsets,
        )

        var swapIndex = 0

        // Run every frame
        extend {
            // Swapping grid information at each step
            val currPositions = positionBuffers[swapIndex % 2]
            val prevPositions = positionBuffers[(swapIndex + 1) % 2]
            swapIndex++

            // Write and sort grid buffers
            updateIndices.run(currPositions)
            gpuSort.sort(currPositions, prevPositions)
            gpuSort.calculateOffsets()

            // Execute compute shader to get new positions
            fieldsSimulation.run(
                cellOffsets = cellOffsets,
                currPositions = currPositions,
                prevPositions = prevPositions,
            )

            // compute shader writes to previous buffer
            val newPositions = prevPositions

            // Draw
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE

            // Draw particles from new positions buffer
            drawer.offsetGeometry(
                geometry = geometry,
                newPositions = newPositions,
                colorBuffer = colorBuffer,
                count = count,
                size = sigma,
            )
        }
        extend(FPSDisplay())
    }
}
