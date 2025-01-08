import org.intellij.lang.annotations.Language
import org.openrndr.Fullscreen
import org.openrndr.KEY_ENTER
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import java.io.File

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        val computeWidth = 50
        val computeHeight = 10
        val particleCount = computeWidth * computeHeight * 32
        val gridSize = 200
        var swapIndex = 0

        val computeShader = ComputeShader.fromCode(File("data/compute-shaders/boids.glsl").readText(), "cs")

        // Vertex buffer which holds the geometry to draw
        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, 4)

        //Create a unit quad
        geometry.put {
            write(Vector3(-1.0, -1.0, 0.0))
            write(Vector3(-1.0, 1.0, 0.0))
            write(Vector3(1.0, -1.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
        }

        // Vertex buffer holding transformation for each particle
        val transformationsBuffer = vertexBuffer(vertexFormat {
            attribute("transform", VertexElementType.MATRIX44_FLOAT32)
        }, particleCount).also {
            it.put {
                repeat(it.vertexCount) {
                    write(transform {
                        translate(drawer.bounds.uniform())
                        rotate(Vector3.UNIT_Z, random(0.0, 360.0))
                        scale(2.0)
                    })
                }
            }
        }

        // Vertex buffer containing velocity and direction for each particle
        val propertiesBuffer = vertexBuffer(vertexFormat {
            attribute("velocity", VertexElementType.VECTOR2_FLOAT32)
            attribute("direction", VertexElementType.VECTOR2_FLOAT32)
        }, particleCount).also {
            // Initialize buffer with velocities and directions
            it.put {
                repeat(it.vertexCount) {
                    // velocity
                    write(Vector2.uniform(-1.0, 1.0))
                    write(Polar(random(0.0, 360.0), 1.0).cartesian)
                }
            }
        }

        val buffers = List(2) {
            vertexBuffer(
                vertexFormat {
                    attribute("pos", VertexElementType.VECTOR3_FLOAT32)
                    attribute("dir", VertexElementType.VECTOR2_FLOAT32)
                }, gridSize * gridSize
            ).also {
                it.put {
                    repeat(it.vertexCount) {
                        // velocity
                        write(Vector3.ZERO)
                        write(Vector2.ZERO)
                    }
                }
            }
        }

        // Define GUI
        val gui = GUI()

        val settings = object {
            @DoubleParameter("cohesion", 0.0, 0.05)
            var cohesion = 0.004

            @DoubleParameter("alignment", 0.0, 0.05)
            var alignment = 0.004

            @DoubleParameter("separation", 0.0, 0.05)
            var separation = 0.004

            @DoubleParameter("target", 0.0, 0.5)
            var target = 0.1
        }

        computeShader.uniform("computeWidth", computeWidth)
        computeShader.uniform("width", width.toDouble())
        computeShader.uniform("height", height.toDouble())
        computeShader.uniform("gridSize", gridSize)
        computeShader.buffer("transformsBuffer", transformationsBuffer)
        computeShader.buffer("propertiesBuffer", propertiesBuffer)
        computeShader.buffer("gridPrev", buffers[0])
        computeShader.buffer("gridNext", buffers[1])

        // Define rendering settings and color buffers for post-processing
        val rt = renderTarget(width, height) {
            colorBuffer()
        }

        val prevFrame = colorBuffer(width, height)
        val currentFrame = colorBuffer(width, height)

        gui.add(settings, "Parameters")

        keyboard.keyDown.listen {
            if (it.key == KEY_ENTER) {
                propertiesBuffer.put {
                    repeat(propertiesBuffer.vertexCount) {
                        // velocity
                        write(Vector2.uniform(-0.05, 0.05))
                        write(
                            Polar(random(0.0, 360.0), 1.0).cartesian
                        )
                    }
                }
            }
        }

        extend(gui)
        extend {

            // Swapping grid information at each step
            val nextBuf = buffers[swapIndex % 2]
            val prevBuf = buffers[(swapIndex + 1) % 2]
            //Clear buffer
            nextBuf.put {
                repeat(gridSize * gridSize) {
                    // velocity
                    write(Vector3.ZERO)
                    write(Vector2.ZERO)
                }
            }
            computeShader.buffer("gridPrev", prevBuf)
            computeShader.buffer("gridNext", nextBuf)
            swapIndex++

            computeShader.uniform("separation", settings.separation)
            computeShader.uniform("alignment", settings.alignment)
            computeShader.uniform("cohesion", settings.cohesion)
            computeShader.uniform("target", settings.target)

            computeShader.uniform("mousePos", mouse.position)

            // Render on external target
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.BLACK)
                fill = ColorRGBa.WHITE.opacify(.1)
                shadeStyle = shadeStyle {
                    vertexTransform =
                        "x_viewMatrix = x_viewMatrix * i_transform;"
                }
                // Draw instances of quad
                vertexBufferInstances(
                    listOf(geometry),
                    listOf(transformationsBuffer),
                    DrawPrimitive.TRIANGLE_STRIP,
                    particleCount
                )
            }

            computeShader.execute(computeWidth, computeHeight)
            rt.colorBuffer(0).copyTo(currentFrame)
            drawer.isolatedWithTarget(rt) {
                shadeStyle = shadeStyle {
                    fragmentPreamble = """
                            vec3 laplacian(in vec2 uv, in sampler2D tex, in vec2 texelSize) {
                                  vec3 rg = vec3(0.0);
                                 
                                  rg += texture(tex, uv + vec2(-1.0, -1.0)*texelSize).rgb * 0.05;
                                  rg += texture(tex, uv + vec2(-0.0, -1.0)*texelSize).rgb * 0.2;
                                  rg += texture(tex, uv + vec2(1.0, -1.0)*texelSize).rgb * 0.05;
                                  rg += texture(tex, uv + vec2(-1.0, 0.0)*texelSize).rgb * 0.2;
                                  rg += texture(tex, uv + vec2(0.0, 0.0)*texelSize).rgb * -1;
                                  rg += texture(tex, uv + vec2(1.0, 0.0)*texelSize).rgb * 0.2;
                                  rg += texture(tex, uv + vec2(-1.0, 1.0)*texelSize).rgb * 0.05;
                                  rg += texture(tex, uv + vec2(0.0, 1.0)*texelSize).rgb * 0.2;
                                  rg += texture(tex, uv + vec2(1.0, 1.0)*texelSize).rgb * 0.05;
                                                
                                  return rg;
                                }
                        """.trimIndent()
                    fragmentTransform = """
                           vec2 texCoord = c_boundsPosition.xy;
                           texCoord.y = 1.0 - texCoord.y;
                           vec3 currentColor = texture(p_currentFrame, texCoord).rgb;
                           vec2 size = textureSize(p_pastFrame, 0);
                           vec3 diffuse = laplacian(texCoord, p_currentFrame, 1.0/size);
                           vec3 prevColor = texture(p_pastFrame, texCoord).rgb;
                        
                           x_fill.rgb = currentColor + diffuse * 0.9 + prevColor * 0.94;
                       """.trimIndent()
                    parameter("currentFrame", currentFrame)
                    parameter("pastFrame", prevFrame)
                }

                rectangle(bounds)
            }

            rt.colorBuffer(0).copyTo(prevFrame)
            // Colorize
            drawer.isolatedWithTarget(rt) {
                shadeStyle = shadeStyle {
                    fragmentTransform = """
                            vec2 texCoord = c_boundsPosition.xy;
                            texCoord.y = 1.0 - texCoord.y;
                            vec2 size = textureSize(p_image, 0);
                            float t = texture(p_image, texCoord).x;
                            
                            vec3 col = mix(vec3(0.0), mix(vec3(2 * t, 0.0, 0.0), vec3(1.0), min(t * 1.0, 1.0)), min(t * 1.0, 1.0));
                            x_fill.rgb =  col;
                        """.trimIndent()
                    parameter("image", prevFrame)
                }
                rectangle(bounds)
            }
            drawer.image(rt.colorBuffer(0))
        }
    }
}
