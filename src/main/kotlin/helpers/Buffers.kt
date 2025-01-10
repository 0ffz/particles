package helpers

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector3

object Buffers {
    fun circleFanGeometry(): VertexBuffer {
        val numSegments = 10
        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, numSegments + 2)

        geometry.put {
            write(Vector3(0.0, 0.0, 0.0))
            val angleStep = (2 * Math.PI) / numSegments
            for (i in 0 ..numSegments) {
                val angle = i * angleStep
                val x = Math.cos(angle)
                val y = Math.sin(angle)
                write(Vector3(x, y, 0.0))
            }
        }
        return geometry
    }

    fun colorBuffer(color: ColorRGBa, count: Int) = vertexBuffer(vertexFormat { color(4) }, count).apply {
        put {
            repeat(count) {
                write(color)
            }
        }
    }

    fun uInt(count: Int) = vertexBuffer(vertexFormat {
        attribute("index", VertexElementType.UINT32)
    }, count)
}
