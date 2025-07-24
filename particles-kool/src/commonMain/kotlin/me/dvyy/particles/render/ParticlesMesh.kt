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
import me.dvyy.particles.compute.helpers.KslInt
import me.dvyy.particles.compute.simulation.SimulationParametersStruct
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
            configRepository.config.map { it.simulation.count }.distinctUntilChanged().collectLatest {
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
                mesh.shader?.uniformStruct("params", ::SimulationParametersStruct)?.set {
                    maxVelocity.set(it.maxVelocity.toFloat())
                    maxForce.set(it.maxForce.toFloat())
                }
            }
        }
    }

    val mesh = Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS, instances = instances).apply {
        val do3dShading = true
        val tintFarAway = configRepository.boxSize.z > 400f
        shader = KslShader("test") {
            val interColor = interStageFloat4()
            val fragPos = interStageFloat4("fragPos")
            val interCenter = interStageFloat3("interCenter")

            vertexStage {
                main {
                    val camData = cameraData()
                    val indexes = storage<KslInt1>("indexesBuffer")
                    val cellIds = storage<KslInt1>("cellIdsBuffer")
                    val positionsBuffer = storage<KslFloat4>("positionsBuffer")
                    val velocitiesBuffer = storage<KslFloat4>("velocitiesBuffer")
                    val typeColorsBuffer = storage<KslFloat4>("typeColorsBuffer")
                    val clusterBuffer = storage<KslInt1>("clusterBuffer")
                    val typesBuffer = storage<KslInt1>("typesBuffer")
                    val radii = storage<KslFloat1>("radii")
                    val colorType = uniformInt1("colorType")
                    val simulationParams = uniformStruct("params", provider = ::SimulationParametersStruct)

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
                    fragPos.input set outPosition
                    interCenter.input set position
                    val maxVelocity = simulationParams.struct.maxVelocity.ksl

                    val baseColor = float4Var(typeColorsBuffer[typesBuffer[offset]])
                    `if`(colorType eq ParticleColor.CLUSTER.ordinal.const) {
                        val clusterId = int1Var(clusterBuffer[offset])
                        baseColor set randomColor(clusterId)
                    }.elseIf(colorType eq ParticleColor.VELOCITY.ordinal.const) {
                        val velocity = float1Var(length(velocitiesBuffer[offset]))
                        baseColor set float4Value(
                            pow(
                                velocity / maxVelocity,
                                1.5f.const
                            ), 0f.const, 0f.const, 1f.const
                        )
                    }/*.elseIf(interColorType.output eq ParticleColor.INDEX.ordinal.const) {
                        baseColor set randomColor()//float4Value(interIndex.output.toFloat1() / 10f.const, 0f.const, 0f.const, 1f.const)
                    }*/
                    interColor.input set baseColor
                }
            }
            fragmentStage {
                main {
                    val uv = float2Var(interCenter.output.xy + 0.5.const)

                    // Choose color based on specified option
                    val baseColor = float4Var(interColor.output)

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
            storage("cellIdsBuffer", buffers.particleGridCellKeys)
//            storage("indexesBuffer", buffers.particleGridCellKeys)
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

    fun KslScopeBuilder.randomColor(hash: KslInt): KslVarVector<KslFloat4, KslFloat1> {
        fun hash(int: KslExpression<KslInt1>) =
            fract(sin(int.toFloat1() * 78.233.const) * 43758.5453.const)

        val color = float4Var(1f.const4)
        `if`(hash eq (-1).const) {
            color set float4Value(0.1f, 0.1f, 0.1f, 1f)
        }.`else` {
            val r = hash(hash)
            val g = hash(hash + 1.const)
            val b = hash(hash + 2.const)
            color set float4Value(r, g, b, 1f.const)
        }
        return color
    }
}
