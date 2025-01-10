package helpers

import org.openrndr.draw.*
import sigma

object Drawing {
    fun Drawer.offsetGeometry(
        geometry: VertexBuffer,
        newPositions: VertexBuffer,
        colorBuffer: VertexBuffer,
        count: Int,
        size: Double,
    ) = isolated {
            shadeStyle = shadeStyle {
                vertexPreamble = """
                        out vec4 c;
                    """.trimIndent()

                // Draw geometry offset by positions
                vertexTransform = """
                    mat4 translationMatrix = mat4($size / 2); // Identity matrix scaled
                    translationMatrix[3] = vec4(i_position, 0.0, 1.0);
                    x_viewMatrix = x_viewMatrix * translationMatrix;
                    c = i_color;
                    """.trimIndent()

                // set color to colorBuffer's color
                fragmentPreamble = "in vec4 c;"
                fragmentTransform = """x_fill.rgba = c;"""
            }

            vertexBufferInstances(
                listOf(geometry),
                listOf(newPositions, colorBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                count
            )
        }
}
