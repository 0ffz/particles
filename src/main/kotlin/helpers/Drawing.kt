package helpers

import dsl.ParticlesConfiguration
import org.intellij.lang.annotations.Language
import org.openrndr.draw.*
import org.openrndr.extra.olive.Olive

object Drawing {
    fun Drawer.offsetGeometry(
        geometry: VertexBuffer,
        newPositions: VertexBuffer,
        types: VertexBuffer,
        configuration: ParticlesConfiguration,
        colorBuffer: VertexBuffer,
        count: Int,
        size: Double,
    ) = isolated {
        shadeStyle = shadeStyle {
            vertexPreamble = glsl(
                """
                    out vec4 color;
                    out uint particleType;
                """.trimIndent()
            )

            // Draw geometry offset by positions
            vertexTransform = glsl(
                """
                    mat4 translationMatrix = mat4($size / 2); // Identity matrix scaled
                    translationMatrix[3] = vec4(i_position, 0.0, 1.0);
                    x_viewMatrix = x_viewMatrix * translationMatrix;
                    color = i_color;
                    particleType = i_particleType;
                    """.trimIndent()
            )

            // set color to colorBuffer's color
            fragmentPreamble = glsl(
                """
                in vec4 color;
                flat in uint particleType;
                
                const vec4[] colors = vec4[](
                    ${configuration.particleTypes.joinToString(", ") { "vec4(${it.color.r}, ${it.color.g}, ${it.color.b}, ${it.color.alpha})" }}
                );
                """.trimIndent()
            )
            fragmentTransform = glsl(
                """
                x_fill.rgba = colors[particleType];
//                x_fill.rgba = color;
                """.trimIndent()
            )
        }

        vertexBufferInstances(
            listOf(geometry),
            listOf(newPositions, colorBuffer, types),
            DrawPrimitive.TRIANGLE_FAN,
            count
        )
    }
}


inline fun glsl(@Language("GLSL") string: String) = string
