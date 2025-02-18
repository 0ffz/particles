package me.dvyy.particles.render

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.vertexAttribFloat3
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import me.dvyy.particles.FieldsBuffers
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Meshes {
    fun particleMeshInstances(count: Int) = MeshInstanceList(count).apply { numInstances = count }

    fun particleMesh(
        buffers: FieldsBuffers,
        instances: MeshInstanceList,
    ) = Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS, instances = instances).apply {
//            shader = KslBlinnPhongShader(KslBlinnPhongShaderConfig {
//                pipeline { cullMethod = CullMethod.NO_CULLING }
////                lightingCfg.ambientLight = AmbientLight.Uniform(MdColor.LIGHT_BLUE tone 400)
//                modelCustomizer = {
//                    val positionsBuffer = storage1d<KslFloat4>("positionsBuffer")
//                    vertexStage {
//                        main {
////                        val modelMat = modelMatrix()
//                            val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
//                            val offset = int1Var(inInstanceIndex.toInt1())// * 4.const)
//                            val positionOffset = positionsBuffer[offset].xyz
////                        val position = float4Value(0f, 0f, 0f, 0f)
////                        outPosition set float4Value(position, 1f.const)
////                            getFloat3Port("worldPos").input(position + positionOffset)
////                            outPosition set camData.viewProjMat * float4Value(
////                                position + positionOffset,
////                                1f.const
////                            )
//                        }
//                    }
//                }
//            }).apply {
//                storage1d("positionsBuffer", positionsBuffer)
//            }
        shader = KslShader("test") {
            val interColor = interStageFloat4()

            vertexStage {
                main {
                    val camData = cameraData()
                    val positionsBuffer = storage1d<KslFloat4>("positionsBuffer")
                    val colorsBuffer = storage1d<KslFloat4>("colorsBuffer")
                    val typeColorsBuffer = storage1d<KslFloat4>("typeColorsBuffer")
                    val typesBuffer = storage1d<KslInt1>("typesBuffer")
                    val radii = storage1d<KslFloat1>("radii")
                    val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
                    val offset = int1Var(inInstanceIndex.toInt1())
                    val type = int1Var(typesBuffer[offset])
                    val positionOffset = positionsBuffer[offset].xyz
                    val radius = float1Var(radii[type])
//                    texCoordsBlock = texCoordAttributeBlock()
                    // Extract the camera’s right and up vectors from the view matrix.
                    // (Assuming viewMat is an orthonormal matrix, its transpose is its inverse.)
                    val viewMat = camData.viewMat
                    val cameraRight = float3Value(viewMat[0].x, viewMat[1].x, viewMat[2].x)
                    val cameraUp = float3Value(viewMat[0].y, viewMat[1].y, viewMat[2].y)
                    //val cameraIn   = float3Value(viewMat[0].z, viewMat[1].z, viewMat[2].z)

                    // Compute the billboard vertex position:
                    // The vertex’s x and y (from the quad geometry) are used to offset along the camera’s right and up directions.
                    val worldPos = positionOffset.times(
                        Vec3f(1f, -1f, 1f).const
                    ) + (cameraRight * position.x * radius) + (cameraUp * position.y * radius)
                    outPosition set camData.viewProjMat * float4Value(worldPos, 1f.const)
                    interColor.input set typeColorsBuffer[type]//colorsBuffer[offset]
                }
            }
            fragmentStage {
                main {
//                    val uv = float2Var(fragPos.output.xy / fragPos.output.w * 0.5.const + 0.5.const)
//                    val texCoordBlock = vertexStage?.findBlock<TexCoordAttributeBlock>()!!
//                    val splatCoords = float2Var( * SPLAT_MAP_SCALE.const)
//                    val centerOffset = float2Var((uv - 0.5f.const) * 2f.const)
//                    val sqrDst = dot(centerOffset, centerOffset)
//                    `if`(sqrDst gt 1f.const) { discard() }
//                    `if`(texCoordsBlock.getTextureCoords().x gt 0.1f.const) {
//                        discard()
//                    }
//                    float4Value(texCoordBlock.getTextureCoords(), 1f, 1f))//
                    colorOutput(interColor.output)//Color("ff0000").toVec4f().const)
                }
            }
        }.apply {
            storage1d("positionsBuffer", buffers.positionBuffer)
            storage1d("colorsBuffer", buffers.colorsBuffer)
            storage1d("typeColorsBuffer", buffers.particleColors)
            storage1d("radii", buffers.particleRadii)
            storage1d("typesBuffer", buffers.particleTypesBuffer)
        }
        generate {
            fillPolygon(generateCirclePoints(20, radius = 1f))
        }
    }

    fun generateCirclePoints(steps: Int, radius: Float = 1f): List<Vec3f> {
        val points = mutableListOf<Vec3f>()
//    points.add(Vec3f(0f, 0f, 0f)) // Center point

        for (i in 0 until steps) {
            val angle = (i.toFloat() / steps) * (2 * PI).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            points.add(Vec3f(x, y, 0f))
        }
        return points
    }

}
