package helpers

import org.intellij.lang.annotations.Language
import org.openrndr.draw.*

object Drawing {
    fun Drawer.offsetGeometry(
        geometry: VertexBuffer,
        newPositions: VertexBuffer,
        colorBuffer: VertexBuffer,
        count: Int,
        size: Double,
    ) = isolated {
        shadeStyle = shadeStyle {
            vertexPreamble = glsl(
                """
                    out vec4 c;
                """.trimIndent()
            )

            // Draw geometry offset by positions
            vertexTransform = glsl(
                """
                    mat4 translationMatrix = mat4($size / 2); // Identity matrix scaled
                    translationMatrix[3] = vec4(i_position, 0.0, 1.0);
                    x_viewMatrix = x_viewMatrix * translationMatrix;
                    c = i_color;
                    """.trimIndent()
            )

            // set color to colorBuffer's color
            fragmentPreamble = glsl("in vec4 c;")
            fragmentTransform = glsl(
                """
                    x_fill.rgba = c;
                    """.trimIndent()
            )
        }

        vertexBufferInstances(
            listOf(geometry),
            listOf(newPositions, colorBuffer),
            DrawPrimitive.TRIANGLE_STRIP,
            count
        )
    }
}


inline fun glsl(@Language("GLSL") string: String) = string
