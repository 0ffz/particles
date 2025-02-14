package template

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.Text
import de.fabmax.kool.modules.ui2.addPanelSurface
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.OrbitInputTransform
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.debugOverlay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
    val count: Int = (1_000_00 / 64) * 64
    val instances = MeshInstanceList(emptyList()).apply {
        numInstances = count
    }
    val width = ctx.windowWidth
    val height = ctx.windowHeight
    val positionsBuffer = StorageBuffer1d(count, GpuType.FLOAT4).apply {
        for (i in 0 until count) {
            this[i] = Vec4f(Random.nextInt(width).toFloat(), -Random.nextInt(height).toFloat(), -10f, 0f)
        }
    }

//    val particlesMesh = Mesh(Attribute.POSITIONS, instances = instances).apply {
//        generate {
//            cube {
//                color = Color("ff0000")
//                size.set(3f, 3f, 3f)
////                radius = 100f
//            }
//        }
//        shader = KslShader("example") {
//            val positionsBuffer = storage1d<KslFloat4>("positionsBuffer")
//            vertexStage {
//                main {
//                    val modelMat = modelMatrix()
//                    val camData = cameraData()
////                    val position = float3Var(vertexAttribFloat3(Attribute.POSITIONS))
//                    val offset = int1Var(inInstanceIndex.toInt1() * 3.const)
//                    val globalPos = float4Var(positionsBuffer[offset]).xyz
//                    outPosition set camData.viewProjMat * modelMat.matrix * float4Value(globalPos, 1f.const)
//                }
//            }
//            fragmentStage {
//                    main {
//                        colorOutput(Color("ff0000").toVec4f().const)
//                    }
//                // fragment shader main function: Is executed once per pixel to compute the output color
////                main {
//////                    val normalColor = float3Var(interNormal.output * 0.5f.const + 0.5f.const)
////                    colorOutput(normalColor * uScale, 1f.const)
////                }
//            }
//        }.apply {
//            storage1d("positionsBuffer", positionsBuffer)
//        }
//    }
    ctx.scenes += scene {
//        setupUiScene(ClearColorDontCare)
        val movement = SimpleMovement
        movement.storage1d("positionsBuffer", positionsBuffer)
        addComputePass(ComputePass(movement, count))

        addMesh(Attribute.POSITIONS, Attribute.NORMALS, instances = instances) {
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
                storage1d("positionsBuffer", positionsBuffer)
            }
            generate {
                // target 1 million, 90 fps, 80% usage
                fillPolygon(generateCirclePoints(10, radius = 1.5f))
//                cube {
//                    size.set(5f, 5f, 5f)
////                    this.steps = 20
////                    radius = 5f
//                }
            }
        }
        setupUiScene(clearColor = ClearColorFill(Color("444444")))
//        addPanelSurface {
//            Text("Hello world!") { }
//        }
//        addColorMesh {
//            generate {
//                cube {
//                    size.set(4f, 4f, 4f)
//                    this.origin.set(Vec3f.ZERO)
//                }
//            }
//            shader = KslPbrShader {}
//        }
//        defaultOrbitCamera()
//        orbitCamera {
//            zoom = 1.0
//            leftDragMethod = OrbitInputTransform.DragMethod.PAN
//        }
    }

// add the debugOverlay. provides an fps counter and some additional debug info
    ctx.scenes += debugOverlay()
//    return
//    ctx.scenes += scene {
//        val sorter = GPUSort
//        val count = 640
//        val keysBuffer = StorageBuffer1d(count, GpuType.INT1).apply {
//            for (i in 0 until count) {
//                this[i] = Random.nextInt(count)
//            }
//        }
//        val indicesBuffer = StorageBuffer1d(count, GpuType.INT1).apply {
//            for (i in 0 until count) {
//                this[i] = i
//            }
//        }
//
////        addComputePass()
////        ComputePass("TEST").apply {
////            this.addTask()
////        }
//        var keys by sorter.storage1d("keys")
//        var indices by sorter.storage1d("indices")
//        var numValues by sorter.uniform1i("numValues")
//        var stage by sorter.uniform1i("stage")
//        var passOfStage by sorter.uniform1i("passOfStage")
//
//        keysBuffer.releaseWith(this)
//        indicesBuffer.releaseWith(this)
//
//        numValues = count
//        keys = keysBuffer
//        indices = indicesBuffer
//
//        val pass = ComputePass("Sorting pass").apply {
//            val numPairs = count.takeHighestOneBit() * 2
//            val numStages = numPairs.countTrailingZeroBits()
//            for (stageIndex in 1..numStages) {
//                for (stepIndex in 0 until stageIndex) {
//                    addTask(sorter, Vec3i(10)).apply {
//                        onBeforeDispatch {
//                            stage = stageIndex
//                            passOfStage = stepIndex
//                        }
//                    }
//                }
//            }
////            for (stageIndex in 0..<numStages) {
////                for (stepIndex in 0..stageIndex) {
////                    val groupWidth = 1 shl (stageIndex - stepIndex)
////                    val groupHeight = 2 * groupWidth - 1
////
////                    addTask(cs, Vec3i(10)).apply {
////                        onBeforeDispatch {
////                            stage = stageIndex //groupWidth
////                            passOfStage = stepIndex//groupHeight
////                            stepIndex
////                        }
////                    }
////                }
////            }
//        }
//        addComputePass(pass)
//
//        launchOnMainThread {
//            keysBuffer.readbackBuffer()
//            indicesBuffer.readbackBuffer()
//            println((0 until count).map { keysBuffer.getI1(it) }.toString())
//            println((0 until count).map { indicesBuffer.getI1(it) }.toString())
//        }
//    }
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
