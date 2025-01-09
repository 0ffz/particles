import extensions.FPSDisplay
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import java.io.File

fun main() = application {
    val gridSize = (2.5 * sigma).toInt()
    val computeWidth = 50
    val computeHeight = 10
    val count = computeWidth * computeHeight * 32

    configure {
        width = 1000
        height = 1000
        windowResizable = true
    }
    program {
        val gridRows = width / gridSize
        val gridCols = height / gridSize

//        val area = drawer.bounds.offsetEdges(0.0)

        val computeShader = ComputeShader.fromCode(File("data/compute-shaders/fields.glsl").readText(), "cs")

        computeShader.apply {
            uniform("gridSize", gridSize)
            uniform("gridRows", gridRows)
            uniform("gridCols", gridCols)
            uniform("epsilon", epsilon)
            uniform("sigma", sigma)
            uniform("dT", deltaT)
        }

//        for (i in 0 until count) {
//            prevPositions[i] += Random.vector2(-randomVel * deltaT, randomVel * deltaT)
//        }

        val positionBuffers = List(2) {
            vertexBuffer(
                vertexFormat {
                    attribute("position", VertexElementType.VECTOR2_FLOAT32)
                }, gridRows * gridCols
            ).also {
                it.put {
                    repeat(it.vertexCount) {
                        write(Vector2.ZERO)
                    }
                }
            }
        }

        val gridIndexesBuffer = vertexBuffer(vertexFormat {
            attribute("index", VertexElementType.UINT32)
        }, count)

        var swapIndex = 0

        extend {
            // Swapping grid information at each step
            val currPositions = positionBuffers[swapIndex % 2]
            val prevPositions = positionBuffers[(swapIndex + 1) % 2]
            swapIndex++


            // Fill grid
            val gridSizesBuffer = vertexBuffer(vertexFormat {
                attribute("size", VertexElementType.UINT32)
                attribute("offset", VertexElementType.UINT32)
            }, gridRows * gridCols).also {
                it.put {
                    repeat(it.vertexCount) {
                        write(0)
                    }
                }
            }


            // Execute compute shader to get new positions
            computeShader.apply {
                buffer("currParticlesBuffer", currPositions)
                buffer("prevParticlesBuffer", prevPositions)
                buffer("gridIndexesBuffer", gridIndexesBuffer)
            }
            computeShader.execute(computeWidth, computeHeight)

            val newPositions = prevPositions // compute shader writes to previous

            // Draw
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE
            drawer.strokeWeight = 0.5

            // Draw circles from buffer positions

            drawer.vertexBuffer(positionBuffers, DrawPrimitive.POINTS)
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
