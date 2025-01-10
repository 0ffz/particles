package helpers

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector3

object Buffers {
    fun squareGeometry(): VertexBuffer {
        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, 4)

        geometry.put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
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
