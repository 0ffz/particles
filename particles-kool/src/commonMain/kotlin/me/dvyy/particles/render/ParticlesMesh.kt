package me.dvyy.particles.render

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.vertexAttribFloat3
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.simulation.FieldsShader.SimulationParameters
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ParticlesMesh(
    val buffers: ParticleBuffers,
    val configRepository: ConfigRepository,
    val settings: AppSettings,
    val scope: CoroutineScope,
) {
    val instances = MeshInstanceList(configRepository.count).apply { numInstances = configRepository.count }

    init {
        scope.launch {
            configRepository.config.map { it.simulation.targetCount }.distinctUntilChanged().collectLatest {
                instances.numInstances = configRepository.count
            }
        }
        scope.launch {
            settings.ui.coloring.collectLatest {
                mesh.shader?.uniform1i("colorType")?.set(it.ordinal)
            }
        }
        scope.launch {
            configRepository.config.map { it.simulation }.distinctUntilChanged().collectLatest {
                mesh.shader?.uniformStruct("params", ::SimulationParameters)?.set {
                    maxVelocity.set(it.maxVelocity.toFloat())
                    maxForce.set(it.maxForce.toFloat())
                }
            }
        }
    }

    val mesh = Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS, instances = instances).apply {
//            shader = KslBlinnPhongShader(KslBlinnPhongShaderConfig {
//                pipeline { cullMethod = CullMethod.NO_CULLING }
////                lightingCfg.ambientLight = AmbientLight.Uniform(MdColor.LIGHT_BLUE tone 400)
//                modelCustomizer = {
//                    val positionsBuffer = storage<KslFloat4>("positionsBuffer")
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
//                storage("positionsBuffer", positionsBuffer)
//            }
        val do3dShading = true
        val tintFarAway = buffers.configRepo.boxSize.z > 400f
        shader = KslShader("test") {
            val interColor = interStageFloat4()
            val fragPos = interStageFloat4("fragPos")
            val interVelocity = interStageFloat1("interVelocity")
            val interCenter = interStageFloat3("interCenter")
            val interClusterId = interStageInt1("interClusterId")
            val interColorType = interStageInt1("interColorType")
            val interMaxVelocity = interStageFloat1("maxVelocity")

            vertexStage {
                main {
                    val camData = cameraData()
                    val positionsBuffer = storage<KslFloat4>("positionsBuffer")
                    val velocitiesBuffer = storage<KslFloat4>("velocitiesBuffer")
                    val colorsBuffer = storage<KslFloat4>("colorsBuffer")
                    val typeColorsBuffer = storage<KslFloat4>("typeColorsBuffer")
                    val clusterBuffer = storage<KslInt1>("clusterBuffer")
                    val typesBuffer = storage<KslInt1>("typesBuffer")
                    val radii = storage<KslFloat1>("radii")
                    val colorType = uniformInt1("colorType")
                    val simulationParams = uniformStruct("params", provider = ::SimulationParameters)

                    val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
                    val offset = int1Var(inInstanceIndex.toInt1())
                    val type = int1Var(typesBuffer[offset])
                    val clusterId = int1Var(clusterBuffer[offset])
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
                    interColor.input set float4Var(typeColorsBuffer[type])
                    fragPos.input set outPosition
                    interCenter.input set position
                    interClusterId.input set clusterId
                    interColorType.input set colorType
                    interVelocity.input set length(velocitiesBuffer[offset])
                    interMaxVelocity.input set simulationParams.struct.maxVelocity.ksl
                }
            }
            fragmentStage {
                main {
                    val uv = float2Var(interCenter.output.xy + 0.5.const)
//                    val screenPosition = float3Var(fragPos.output.xyz / fragPos.output.w * 0.5.const + 0.5.const)
//                    val texCoordBlock = vertexStage?.findBlock<TexCoordAttributeBlock>()!!

//                    val splatCoords = float2Var( * SPLAT_MAP_SCALE.const)
                    val centerOffset = float2Var((uv - 0.5f.const) * 2f.const)
                    val sqrDst = dot(centerOffset, centerOffset)
//                    `if`(sqrDst gt 1f.const) { discard() }
//                    `if`(texCoordsBlock.getTextureCoords().x gt 0.1f.const) {
//                        discard()
//                    }
//                    float4Value(texCoordBlock.getTextureCoords(), 1f, 1f))//

                    // Choose color based on specified option
                    val baseColor = float4Var(interColor.output)
                    `if`(interColorType.output eq ParticleColor.CLUSTER.ordinal.const) {
                        val cluster = interClusterId.output
                        fun hash(int: KslExpression<KslInt1>) =
                            fract(sin(int.toFloat1() * 78.233.const) * 43758.5453.const)

                        `if`(cluster eq (-1).const) {
                            baseColor set float4Value(0.1f, 0.1f, 0.1f, 1f)
                        }.`else` {
                            val r = hash(cluster)
                            val g = hash(cluster + 1.const)
                            val b = hash(cluster + 2.const)
                            baseColor set float4Value(r, g, b, 1f.const)
                        }
                    }.elseIf(interColorType.output eq ParticleColor.VELOCITY.ordinal.const) {
                        baseColor set float4Value(pow(interVelocity.output / (interMaxVelocity.output + 1f.const), 1.5f.const), 0f.const, 0f.const, 1f.const)
                    }

                    // TODO FORCE color
                    // Update the color buffer based on the magnitude of the net force
//                    colors[id] = float4Value(
//                        log(length(nextForce)) / (2f.const * log(1000f.const)),
//                        0f.const, 0f.const, 1f.const
//                    )

                    // Tint color in 3d and apply sphere-like shadow
                    val color = baseColor.run {
                        if (tintFarAway) {
                            val depth = float1Var(
                                clamp(fragPos.output.z / fragPos.output.w * 2000f.const, 0.6f.const, 1f.const)
                            )
                            times(float4Value(depth, depth, depth, 1f.const))
                        } else this
                    }.run {
                        if (do3dShading) {
                            val leftShiftedCenter = float2Var((uv - float2Value(0f.const, 1f.const)))
                            val dist = float1Var(1f.const - 0.1f.const * dot(leftShiftedCenter, leftShiftedCenter))
                            times(float4Value(dist, dist, dist, 1f.const))
                        } else this
                    }

                    colorOutput(color)
                }
            }
        }.apply {
            storage("positionsBuffer", buffers.positionBuffer)
            storage("velocitiesBuffer", buffers.velocitiesBuffer)
            storage("colorsBuffer", buffers.colorsBuffer)
            storage("typeColorsBuffer", buffers.particleColors)
            storage("radii", buffers.particleRadii)
            storage("typesBuffer", buffers.particleTypesBuffer)
            storage("clusterBuffer", buffers.clustersBuffer)

        }
        generate {
//            fillPolygon(listOf(Vec3f(1f, 0f, 0f), Vec3f(1f, 1f, 0f), Vec3f(0f, 1f, 0f), Vec3f(0f, 0f, 0f)))
            fillPolygon(generateCirclePoints(20, radius = 1f))
        }
    }

    fun generateCirclePoints(steps: Int, radius: Float = 1f): List<Vec3f> {
        val points = mutableListOf<Vec3f>()

        for (i in 0 until steps) {
            val angle = (i.toFloat() / steps) * (2 * PI).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            points.add(Vec3f(x, y, 0f))
        }
        return points
    }
}
