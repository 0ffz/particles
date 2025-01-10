import org.intellij.lang.annotations.Language
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() = application {
    configure {
        width = 770
        height = 578
    }
    program {
        // -- create the vertex buffer
        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, 4)

        // -- fill the vertex buffer with vertices for a unit quad
        geometry.put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
        }

        // -- create the secondary vertex buffer, which will hold transformations
        val transforms = vertexBuffer(vertexFormat {
            position(2)
        }, 1000)

        val area = drawer.bounds.offsetEdges(-100.0)
        // -- fill the transform buffer
        transforms.put {
            repeat(transforms.vertexCount) {
                write(Random.point(area))
//                    rotate(Vector3.UNIT_Z, Math.random() * 360.0)
//                    scale(Math.random() * 30.0)
            }
        }
        transform {
            scale(2.0)
        }
        extend {
            drawer.fill = ColorRGBa.PINK.opacify(0.25)
            drawer.isolated {

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    mat4 translationMatrix = mat4(10.0); // Identity matrix
                    translationMatrix[3] = vec4(i_position, 0.0, 1.0); 
                    x_viewMatrix = x_viewMatrix * translationMatrix; //* vec4(i_position, 0.0, 1.0);
                    """.trimIndent()
            }
            drawer.vertexBufferInstances(listOf(geometry), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, 1000)
            }
        }
    }
}
