package me.dvyy.particles.render

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslComputeShader
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
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
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
                colorShader.uniform1i("colorType").set(it.ordinal)
            }
        }
        scope.launch {
            settings.ui.recolorGradient.collectLatest {
                colorShader.uniform1i("recolorGradient").set(it.ordinal)
            }
        }
        scope.launch {
            configRepository.config.map { it.simulation }.distinctUntilChanged().collectLatest {
                colorShader.uniformStruct("params", ::SimulationParametersStruct).set {
                    maxVelocity.set(it.maxVelocity.toFloat())
                    maxForce.set(it.maxForce.toFloat())
                }
            }
        }
    }

    val colorShader = KslComputeShader("Color particles") {
        val simulationParams = uniformStruct("params", provider = ::SimulationParametersStruct)
        val colorType = uniformInt1("colorType")
        val recolorGradient = uniformInt1("recolorGradient")

        // Web: Max 8 storage buffers, separate into one shader per coloring type (which get swapped as needed) if we ever need more
        val forcesBuffer = storage<KslFloat4>("forcesBuffer")
        val typeColorsBuffer = storage<KslFloat4>("typeColorsBuffer")
        val colorsBuffer = storage<KslFloat4>("colorsBuffer")
        val clusterBuffer = storage<KslInt1>("clusterBuffer")
        val typesBuffer = storage<KslInt1>("typesBuffer")
        val localNeighboursBuffer = storage<KslFloat1>("localNeighboursBuffer")
        val velocitiesBuffer = storage<KslFloat4>("velocitiesBuffer")

        computeStage(WORK_GROUP_SIZE) {
            main {
                val offset = int1Var(inGlobalInvocationId.x.toInt1())
                val prevColor = float4Var(colorsBuffer[offset])
                val newColor = float4Var(typeColorsBuffer[typesBuffer[offset]]) // default to particle color
                val gradient = Gradients.HEAT
                fun KslScopeBuilder.particleColor(
                    gradient: (KslExprFloat1) -> KslExprFloat4,
                ) {
                    `if`(colorType eq ParticleColor.CLUSTER.ordinal.const) {
                        val clusterId = int1Var(clusterBuffer[offset])
                        newColor set randomColor(clusterId)
                    }.elseIf(colorType eq ParticleColor.VELOCITY.ordinal.const) {
                        val maxVelocity = simulationParams.struct.maxVelocity.ksl
                        val velocity = float1Var(length(velocitiesBuffer[offset]))
                        val input = clamp(velocity / maxVelocity, 0f.const, 1f.const)
                        newColor set gradient(input)
                    }.elseIf(colorType eq ParticleColor.FORCE.ordinal.const) {
                        val maxForce = simulationParams.struct.maxForce.ksl
                        val force = float1Var(length(forcesBuffer[offset]))
                        val input = clamp(pow(force / log(maxForce), 0.5f.const), 0f.const, 1f.const)
                        newColor set gradient(input)
                    }.elseIf(colorType eq ParticleColor.NEIGHBOURS.ordinal.const) {
                        newColor set gradient(clamp(localNeighboursBuffer[offset] / 2f.const, 0f.const, 1f.const))
                    }
                }

                particleColor { gradient.recolor(it) }

                // mix old and new color such that transition happens more slowly, avoiding flickering
                val mix = mix(prevColor, newColor, 0.1f.const)
                colorsBuffer[offset] = mix
            }
        }
    }.apply {
        storage("velocitiesBuffer", buffers.velocitiesBuffer)
        storage("forcesBuffer", buffers.forcesBuffer)
        storage("colorsBuffer", buffers.colorsBuffer)
        storage("localNeighboursBuffer", buffers.localNeighboursBuffer)
        storage("typeColorsBuffer", buffers.particleColors)
        storage("radii", buffers.particleRadii)
        storage("typesBuffer", buffers.particleTypesBuffer)
        storage("clusterBuffer", buffers.clustersBuffer)
        storage("cellIdsBuffer", buffers.particleGridCellKeys)
    }

    val mesh = Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS, instances = instances).apply {
        val do3dShading = true
        val tintFarAway = configRepository.boxSize.z > 400f
        shader = KslShader("test") {
            val interColor = interStageFloat4()
            val fragPos = interStageFloat4("fragPos")
            val interCenter = interStageFloat3("interCenter")

            val camData = cameraData()

            val positionsBuffer = storage<KslFloat4>("positionsBuffer")
            val typesBuffer = storage<KslInt1>("typesBuffer")
            val radii = storage<KslFloat1>("radii")
            val colorsBuffer = storage<KslFloat4>("colorsBuffer")

            vertexStage {
                main {
                    val offset = int1Var(inInstanceIndex.toInt1())
                    val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
                    val type = int1Var(typesBuffer[offset])
                    val positionOffset = positionsBuffer[offset].xyz
                    val radius = float1Var(radii[type])
//                    texCoordsBlock = texCoordAttributeBlock()
                    // Extract the camera’s right and up vectors from the view matrix.
                    // (Assuming viewMat is an orthonormal matrix, its transpose is its inverse.)
                    val viewMat = camData.viewMat

                    // We rotate cameraUp by a very small angle to avoid z-fighting in 2d scenes.
                    val angle = 0.02f
                    val angleSin = sin(angle).const
                    val angleCos = cos(angle).const
                    val rotMat = mat3Value(
                        float3Value(1f.const, 0f.const, 0f.const),
                        float3Value(0f.const, angleCos, -angleSin),
                        float3Value(0f.const, angleSin, angleCos)
                    )

                    val cameraRight = float3Value(viewMat[0].x, viewMat[1].x, viewMat[2].x)
                    val cameraUp = float3Value(viewMat[0].y, viewMat[1].y, viewMat[2].y).times(rotMat)
                    //val cameraIn   = float3Value(viewMat[0].z, viewMat[1].z, viewMat[2].z)

                    // Compute the billboard vertex position:
                    // The vertex’s x and y (from the quad geometry) are used to offset along the camera’s right and up directions.

                    // TODO toggle for 3d sphere rendering
//                    outPosition set camData.viewProjMat * float4Value(
//                        position * radius + positionOffset.times(Vec3f(1f, -1f, 1f).const),
//                        1f.const
//                    )
                    // Billboard rotation
                    val worldPos = positionOffset.times(
                        Vec3f(1f, -1f, 1f).const
                    ) + (cameraRight * position.x * radius) + (cameraUp * position.y * radius)
                    outPosition set camData.viewProjMat * float4Value(worldPos, 1f.const)

                    fragPos.input set outPosition
                    interCenter.input set position
                    interColor.input set colorsBuffer[offset]
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
            storage("radii", buffers.particleRadii)
            storage("typesBuffer", buffers.particleTypesBuffer)
            storage("colorsBuffer", buffers.colorsBuffer)
        }
        generate {
//            icoSphere {
//                steps = 2
//            }
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
