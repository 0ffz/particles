import extensions.FPSDisplay
import helpers.Buffers
import helpers.Drawing.offsetGeometry
import kotlinx.coroutines.channels.Channel
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.drawThread
import org.openrndr.draw.launch
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.extra.noise.Random
import org.openrndr.internal.finish
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
//        windowResizable = true
        vsync = false
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

        window.presentationMode = PresentationMode.MANUAL

        var paused = false
        val manualFrames = Channel<Unit>()
        val drawRateBias = 500 // Higher number means preferring drawing to simulation performance

        keyboard.keyDown.listen {
            when (it.key) {
                KEY_SPACEBAR -> {
                    paused = !paused
                    manualFrames.trySend(Unit)
                }

                KEY_ARROW_RIGHT -> {
                    if (paused) {
//                        lastRendered = System.currentTimeMillis()
                        manualFrames.trySend(Unit)
                    }
                }
            }
        }
        keyboard.keyRepeat.listen {
            when (it.key) {
                KEY_ARROW_RIGHT -> {
                    if (paused) {
                        manualFrames.trySend(Unit)
                    }
                }
            }
        }
        var step = 0
        // Run on draw request
        extend {
            // compute shader writes to previous buffer
            val newPositions = positionBuffers[(swapIndex + 1) % 2]

            // Draw
//            val drawer = drawThread.drawer
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

        extend(FPSDisplay({ step }))
        var lastRendered = System.nanoTime()
        val drawThread = drawThread()
        drawThread.launch {
            while(true) {
                if(paused) manualFrames.receive()
                // Swapping grid information at each step
                val currPositions = positionBuffers[swapIndex % 2]
                val prevPositions = positionBuffers[(swapIndex + 1) % 2]
                swapIndex++
                step++

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
                val currTime = System.nanoTime()
                if(currTime - lastRendered > 1e9 / drawRateBias) {
                    lastRendered = System.nanoTime()
                    window.requestDraw()
                    finish()
                }
            }
        }
    }
}
