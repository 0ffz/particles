import extensions.FPSDisplay
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector3
import java.io.File

fun main() = application {
    val gridSize = (2.5 * sigma).toInt()
    val computeWidth = 50
    val computeHeight = 50
    val count = computeWidth * computeHeight * 32

    configure {
        width = 1000
        height = 1000
        windowResizable = true
    }
    program {
        val gridRows = width / gridSize
        val gridCols = height / gridSize

        val area = drawer.bounds.offsetEdges(-10.0)

        val fieldsShader = ComputeShader.fromCode(File("data/compute-shaders/fields.comp").readText(), "fields")
//        val debugShader = ComputeShader.fromCode(File("data/compute-shaders/highlightShader.comp").readText(), "debug")

        fieldsShader.apply {
            uniform("gridSize", gridSize)
            uniform("gridRows", gridRows)
            uniform("gridCols", gridCols)
            uniform("epsilon", epsilon)
            uniform("sigma", sigma)
            uniform("dT", deltaT)
            uniform("count", count)
        }

//        for (i in 0 until count) {
//            prevPositions[i] += Random.vector2(-randomVel * deltaT, randomVel * deltaT)
//        }

        // Initialize buffers

        val positionBuffers = List(2) { listId ->
            vertexBuffer(
                vertexFormat {
                    position(2)
                },
                count,
            ).also {
                Random.resetState()
                it.put {
                    repeat(it.vertexCount) {
                        write(Random.point(area))
                    }
                }
            }
        }
        val particle2CellKey = vertexBuffer(vertexFormat {
            attribute("index", VertexElementType.UINT32)
        }, count)

        val sortedParticleIndices = vertexBuffer(vertexFormat {
            attribute("index", VertexElementType.UINT32)
        }, count)

        val cellOffsets = vertexBuffer(vertexFormat {
            attribute("index", VertexElementType.UINT32)
        }, count)

        var swapIndex = 0

        fieldsShader.apply {
            buffer("particle2CellKeyBuffer", particle2CellKey)
            buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
            buffer("cellStartIndicesBuffer", cellOffsets)
        }

        val colorBuffer = vertexBuffer(vertexFormat {
//            position(2)
            color(4)
        }, count).apply {
            put {
                repeat(count) {
                    write(ColorRGBa.BLACK)
                }
            }
        }

        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, 4)

        geometry.put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
        }

        var isFirst = true
        extend {
            // Swapping grid information at each step
            val currPositions = positionBuffers[swapIndex % 2]
            val prevPositions = positionBuffers[(swapIndex + 1) % 2]
            swapIndex++

//            if (isFirst) {
            // Write and sort grid buffers
            UpdateIndices.updateIndices(
                gridSize = gridSize,
                gridCols = gridCols,
                keys = particle2CellKey,
                indices = sortedParticleIndices,
                positions = prevPositions
            )
            GPUSort.sort(
                keys = particle2CellKey,
                values = sortedParticleIndices
            )

            GPUSort.calculateOffsets(
                keys = particle2CellKey,
                offsets = cellOffsets,
                numValues = count
            )
//            }
//            isFirst = false

            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE
//            drawer.isolated {
//                drawer.shadeStyle = shadeStyle {
//                    vertexTransform = """
//                    mat4 translationMatrix = mat4(2.0); // Identity matrix scaled
//                    translationMatrix[3] = vec4(gl_InstanceID, i_index, 0.0, 1.0);
//                    x_viewMatrix = x_viewMatrix * translationMatrix;
//                    """.trimIndent()
//
//                }
//
//                drawer.vertexBufferInstances(
//                    listOf(geometry),
//                    listOf(sortedParticleIndices),
//                    DrawPrimitive.TRIANGLE_STRIP,
//                    count
//                )
//            }
//            debugShader.apply {
//                uniform("mouse", mouse.position)
//                uniform("gridSize", gridSize)
//                uniform("gridRows", gridRows)
//                uniform("gridCols", gridCols)
//                uniform("count", count)
//                uniform("epsilon", epsilon)
//                uniform("sigma", sigma)
//                buffer("particle2CellKeyBuffer", particle2CellKey)
//                buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
//                buffer("colorBuffer", colorBuffer)
//                buffer("currParticlesBuffer", currPositions)
//                execute(computeWidth, computeHeight)
//            }

            fieldsShader.apply {
                buffer("sortedParticleIndicesBuffer", sortedParticleIndices)
                buffer("cellStartIndicesBuffer", cellOffsets)
                buffer("currParticlesBuffer", currPositions)
                buffer("prevParticlesBuffer", prevPositions)
            }
            // Execute compute shader to get new positions
            fieldsShader.execute(computeWidth, computeHeight)

            val newPositions = prevPositions // compute shader writes to previous

            // Draw
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE
//            drawer.strokeWeight = 0.5

            // Draw circles from buffer positions
            drawer.isolated {
                drawer.shadeStyle = shadeStyle {
                    vertexPreamble = """
                        out vec4 c;
                    """.trimIndent()

                    vertexTransform = """
                    mat4 translationMatrix = mat4($sigma / 2); // Identity matrix scaled
                    translationMatrix[3] = vec4(i_position, 0.0, 1.0);
                    x_viewMatrix = x_viewMatrix * translationMatrix;
                    c = i_color;
                    """.trimIndent()
                    // set color to colorBuffer's color
                    fragmentPreamble = "in vec4 c;"
                    fragmentTransform = """x_fill.rgba = c;"""
//                    fragmentTransform = """x_fill.rgb = va_color.rgb;"""
                }

                drawer.vertexBufferInstances(
                    listOf(geometry),
                    listOf(newPositions, colorBuffer),
                    DrawPrimitive.TRIANGLE_STRIP,
                    count
                )
            }
//            drawer.vertexBufferInstances(listOf(geometry), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, 1000)

//            drawer.vertexBuffer(positionBuffers, DrawPrimitive.POINTS)
            // draw forces as lines in one batch
//            if (showForceLines) {
//                val visualForceClamp = 100.0
//                drawer.lineSegments(forces.flatMapIndexed { i, force ->
//                    listOf(
//                        positions[i],
//                        positions[i] + (force * 10.0).clamp(
//                            Vector2(-visualForceClamp, -visualForceClamp),
//                            Vector2(visualForceClamp, visualForceClamp)
//                        )
//                    )
//                })
//            }
        }
        extend(FPSDisplay())
    }
}
