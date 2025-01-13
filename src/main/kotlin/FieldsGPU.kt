import extensions.FPSDisplay
import helpers.Buffers
import helpers.Drawing.offsetGeometry
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.delay
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.internal.finish
import org.openrndr.panel.controlManager
import org.openrndr.panel.style.*
import shaders.FieldsSimulation
import shaders.GPUSort
import shaders.UpdateIndices
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.nanoseconds

fun main() = application {
    configure {
        width = 1000
        height = 600
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        windowResizable = true
        vsync = false
    }

    program {
//        val font = loadFont("data/fonts/Jetbrains Mono.otf", 16.0)
//        drawer.fontMap = font
//        drawer.fontMap.addTo() = font
        // Create simulation settings and attach to the gui
        val settings = SimulationSettings()
        controlManager {
            this.controlManager.fontManager
        }
        val test = styleSheet {
//            background = Color.RGBa(ColorRGBa.PINK)
//            color = Color.RGBa(ColorRGBa.BLACK)
//            this.fontFamily = "data/fonts/Jetbrains_Mono.otf"
        }
        val gui = GUI(
            defaultStyles = defaultStyles(
                controlFontSize = 17.0,
            ) + test
        ).apply {
            compartmentsCollapsedByDefault = false

            add(SimulationConstants)
            add(settings)
        }
        extend(gui) // Load saved values right away
        val gridSize = (2.5 * SimulationConstants.sigma).toInt()
        val computeWidth = sqrt(targetCount / 32.0).toInt()
        val computeHeight = computeWidth
        val count = computeWidth * computeHeight * 32

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
            settings = settings,
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
        extend(FPSDisplay { step })
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
                size = SimulationConstants.sigma,
            )
        }

        var lastRendered = System.nanoTime()
        var lastSimulation = lastRendered
        val drawThread = drawThread()
        val targetSimulationSpeed = 60.0
        var restarting = false
        drawThread.launch {
            while (true) {
                if(restarting) break
                // match target sim speed
                if (paused) manualFrames.receive()
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
                if (currTime - lastRendered > 1e9 / drawRateBias) {
                    lastRendered = System.nanoTime()
                    window.requestDraw()
                    finish()
                }

                val curr = System.nanoTime()
                val timeBetweenSimulations = 1e9 / targetSimulationSpeed
                if (curr - lastSimulation < 1e9 / targetSimulationSpeed) {
                    delay(timeBetweenSimulations.nanoseconds)
                } else {
                    lastSimulation = System.nanoTime()
                }
            }
        }
        SimulationConstants.restartEvent.listen {
            println("Restarting simulation...")
            restarting = true
        }
    }
}
