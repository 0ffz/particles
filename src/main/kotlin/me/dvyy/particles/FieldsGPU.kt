package me.dvyy.particles

import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import me.dvyy.particles.dsl.ParticlesConfiguration
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.helpers.Drawing.offsetGeometry
import me.dvyy.particles.shaders.FieldsSimulation
import me.dvyy.particles.shaders.GPUSort
import me.dvyy.particles.shaders.UpdateIndices
import org.openrndr.Extension
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import org.openrndr.internal.finish
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FieldsGPU(
    val screenSize: Rectangle,
    val config: ParticlesConfiguration,
    val onResetRequested: () -> Unit,
) : Extension {
    override var enabled = true

    val computeWidth = (SimulationConstants.targetCount / 32.0).toInt()
    val count = computeWidth * 32

    // Due to our grid datastructure below, we need at least as many particles as there are grid cells,
    // for low particle counts we raise the grid cell size enough to fit in the particle count
    val gridSize = run {
        val smallestSize = 2.5 * SimulationConstants.minGridSize
        val cols = (screenSize.width / smallestSize).toInt()
        val rows = (screenSize.height / smallestSize).toInt()
        if (rows * cols > count) {
            sqrt((screenSize.width * screenSize.height) / count) + 1.0
        } else smallestSize
    }

    //    val computeHeight = computeWidth

    val simulationThread = drawThread() // need access to gpu buffers so has to be a draw thread

    //    val area = drawer.bounds//.offsetEdges(-10.0)
    val gridCols = (screenSize.width / gridSize).toInt().also { println("$it cols") }
    val gridRows = (screenSize.height / gridSize).toInt().also { println("$it rows") }
    val area = Rectangle(0.0, 0.0, gridCols * gridSize, gridRows * gridSize)

    init {
        println("Total grid cells: ${gridCols * gridRows}, total particles: $count")
    }

    // Initialize buffers
    val positionBuffers = List(2) { listId ->
        vertexBuffer(
            vertexFormat {
                position(2)
                attribute("velocity", VertexElementType.VECTOR2_FLOAT32)
            },
            count,
        )
    }/*.apply {
            Random.resetState()
            put {
                repeat(vertexCount) {
                    write(Random.point(area))
                }
            }
        }
    }*/

    fun initializeRandomPositions() {
        positionBuffers.forEach { buffer ->
//            Random.resetState()
            buffer.put {
                repeat(buffer.vertexCount) {
                    write(Random.point(area))
//                    write(Vector2.ZERO)
                    write(
                        Random.vector2(
                            min = -SimulationSettings.startingVelocity,
                            max = SimulationSettings.startingVelocity
                        )
                    )
                }
            }
        }
    }

    val particle2CellKey = Buffers.uInt(count)

    //        val sortedParticleIndices = Buffers.uInt(count)
    val cellOffsets = Buffers.uInt(count)
    val particleTypes = Buffers.uInt(count, "particleType").apply {
        put {
            val distTotal = config.particleTypes.sumOf { it.distribution }
            val counts = config.particleTypes
                .map { ((it.distribution / distTotal) * count).toInt() }

            // Add particles based on distribution, ensuring we always get exactly `count` particles
            var type = 0
            var offset = 0
            repeat(count) {
                write(type)
                if (it - offset >= counts[type] && it != counts.lastIndex) {
                    type++
                    offset = it
                }
            }
        }
    }
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
        settings = SimulationSettings,
        count = count,
        computeWidth = computeWidth,
        particle2CellKey = particle2CellKey,
        cellOffsets = cellOffsets,
        colorBuffer = colorBuffer,
        config = config,
    )

    val gpuSort = GPUSort(
        numValues = count,
//            indices = sortedParticleIndices,
        sortByKey = particle2CellKey,
        offsets = cellOffsets,
        types = particleTypes,
    )

    var swapIndex = 0

    var paused = false
    val manualFrames = Channel<Unit>()
    val drawRateBias = 1000 // Higher number means preferring drawing to simulation performance
    var stopping = false

    override fun shutdown(program: Program) {
        println("Destroying buffers...")
        paused = true
        stopping = true
        updateIndices.computeShader.destroy()
        fieldsSimulation.fieldsShader.destroy()
        gpuSort.sorterShader.destroy()
        gpuSort.offsetsShader.destroy()
        positionBuffers.forEach { it.destroy() }
        particle2CellKey.destroy()
        cellOffsets.destroy()
        particleTypes.destroy()
        colorBuffer.destroy()
        geometry.destroy()
        simulationThread.dispatcher.cancel()
    }

    override fun setup(program: Program): Unit = with(program) {
        initializeRandomPositions()
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

        var lastRendered = System.nanoTime()
        var lastSimulation = lastRendered
        val targetSimulationSpeed = 60.0
        program.ended.listen {
            stopping = true
        }

        simulationThread.launch {
            while (true) {
                if (stopping) break
                // match target sim speed
                if (paused) manualFrames.receive()
                // Swapping grid information at each step
                val currPositions = positionBuffers[swapIndex % 2]
                val prevPositions = positionBuffers[(swapIndex + 1) % 2]
                swapIndex++
                SimulationSettings.step++

                // Write and sort grid buffers
                updateIndices.run(currPositions)
                gpuSort.sort(currPositions, prevPositions)
                gpuSort.calculateOffsets()
                fun readBufferToIntArray(bufferToRead: VertexBuffer): IntArray {
                    val byteBuffer =
                        ByteBuffer.allocateDirect(bufferToRead.vertexCount * bufferToRead.vertexFormat.size)
                    byteBuffer.order(ByteOrder.nativeOrder())
                    bufferToRead.read(byteBuffer)
                    byteBuffer.rewind()
                    val intArray = IntArray(bufferToRead.vertexCount)
                    byteBuffer.asIntBuffer().get(intArray)
                    return intArray
                }


//                println(readBufferToIntArray(particle2CellKey).contentToString())
//                println(readBufferToIntArray(cellOffsets).contentToString())
//
//                return@launch

                // Execute compute shader to get new positions
                fieldsSimulation.run(
                    cellOffsets = cellOffsets,
                    currPositions = currPositions,
                    prevPositions = prevPositions,
                    particleTypes = particleTypes
                )
                val currTime = System.nanoTime()
                if (currTime - lastRendered > 1e9 / drawRateBias) {
                    window.requestDraw()
                    lastRendered = System.nanoTime()
                    finish()
                }

//                val curr = System.nanoTime()
//                val timeBetweenSimulations = 1e9 / targetSimulationSpeed
//                if (curr - lastSimulation < 1e9 / targetSimulationSpeed) {
//                    delay(timeBetweenSimulations.nanoseconds)
//                } else {
//                    lastSimulation = System.nanoTime()
//                }
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
//        drawer.isolated {
//            drawer.shadeStyle = shadeStyle {
//                vertexTransform = """
//                mat4 translationMatrix = mat4(1.0); // Identity matrix scaled
//                translationMatrix[3] = vec4(0.9 * $width * gl_InstanceID / $count, 0.9 * $height * i_index / ${gridCols * gridRows}, 0.0, 1.0);
//                x_viewMatrix = x_viewMatrix * translationMatrix;
//                """.trimIndent()
//
//            }
//
//            drawer.vertexBufferInstances(
//                listOf(geometry),
//                listOf(cellOffsets),
//                DrawPrimitive.TRIANGLE_FAN,
//                count
//            )
//        }
//        return
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
            types = particleTypes,
            configuration = config,
            colorBuffer = colorBuffer,
            count = count,
        )
    }
}

