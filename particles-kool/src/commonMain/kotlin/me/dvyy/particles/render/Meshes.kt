package me.dvyy.particles.render

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.lang.KslFloat4
import de.fabmax.kool.modules.ksl.lang.plus
import de.fabmax.kool.modules.ksl.lang.times
import de.fabmax.kool.modules.ksl.lang.toInt1
import de.fabmax.kool.modules.ksl.lang.xyz
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.StorageBuffer1d
import de.fabmax.kool.pipeline.vertexAttribFloat3
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.util.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Meshes {
    fun particleMeshInstances(count: Int) = MeshInstanceList(count).apply { numInstances = count }

    fun particleMesh(positions: StorageBuffer1d, instances: MeshInstanceList) = Mesh(Attribute.POSITIONS, Attribute.NORMALS, instances = instances).apply {
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
                vertexStage {
                    main {
                        val camData = cameraData()
                        val positionsBuffer = storage1d<KslFloat4>("positionsBuffer")
                        val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
                        val offset = int1Var(inInstanceIndex.toInt1())
                        val positionOffset = positionsBuffer[offset].xyz
                        outPosition set camData.viewProjMat * float4Value(
                            position + positionOffset,
                            1f.const
                        )
                    }
                }
                fragmentStage {
                    main {
                        colorOutput(Color("ff0000").toVec4f().const)
                    }
                }
            }.apply {
                storage1d("positionsBuffer", positions)
            }
            generate {
                fillPolygon(generateCirclePoints(10, radius = 1.5f))
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
