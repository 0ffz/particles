package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.dsl.Parameter
import me.dvyy.particles.dsl.ParticlesConfig

class FieldsShader(
    val config: ParticlesConfig,
) {
    val shader = KslComputeShader("Fields") {
        computeStage(WORK_GROUP_SIZE) {
            // Uniforms
            val gridSize = uniformFloat1("gridSize")
            val gridRows = uniformInt1("gridRows")
            val gridCols = uniformInt1("gridCols")
            val gridDepth = uniformInt1("gridDepth")
            val dT = uniformFloat1("dT")
            val count = uniformInt1("count")
            val maxForce = uniformFloat1("maxForce")
            val maxVelocity = uniformFloat1("maxVelocity")

            config.configurableUniforms.forEach {
                it.toKsl(program)
            }

            // Storage buffers
            val particle2CellKey = storage1d<KslInt1>("particle2CellKey")
            // (binding = 1 is omitted, as in the GLSL code)
            val cellOffsets = storage1d<KslInt1>("cellOffsets")
            val currPositions = storage1d<KslFloat4>("currPositions")
            val prevPositions = storage1d<KslFloat4>("prevPositions")
            val currVelocities = storage1d<KslFloat4>("currVelocities")
            val prevVelocities = storage1d<KslFloat4>("prevVelocities")
            val prevForces = storage1d<KslFloat4>("prevForces")

            val colors = storage1d<KslFloat4>("colors")
            val particleTypes = storage1d<KslInt1>("particleTypes")

            //TODO actually implement for generic function types
            val lennardJones = LennardJones()
            with(lennardJones) {
                createFunction()
            }

            // Helper: compute cell id from grid coordinates (cell id = x + y * gridCols)
            val cellId = functionInt1("cellId") {
                val xGrid = paramInt1("xGrid")
                val yGrid = paramInt1("yGrid")
                val gridCols = paramInt1("gridCols")
                body {
                    xGrid + (yGrid * gridCols)
                }
            }

            main {
                // Get the particle id from the global invocation (using only x as in GLSL)
                val id = int1Var(inGlobalInvocationId.x.toInt1())

                // Load current particle properties
                // Extract the 2D position from the stored vec4
                val position = float3Var(currPositions[id].xyz) //p(t)
                val velocity = float3Var(currVelocities[id].xyz) //v(t)
                val particleType = int1Var(particleTypes[id])
//                val force = TODO() //a(t)

                // Compute grid indices based on the particle position
                val xGrid = int1Var((position.x / gridSize).toInt1())
                val yGrid = int1Var((position.y / gridSize).toInt1())
                // Compute the base cell id (convert to int for bounds checking)
                val cellId = int1Var(cellId(xGrid, yGrid, gridCols))

                // a(t) TODO change to a(t + dT)
                val netForce = float3Var(Vec3f.ZERO.const)

                // Loop over neighboring grid cells (x and y offsets from -1 to 1)
                fori((-1).const, 2.const) { x ->
                    fori((-1).const, 2.const) { y ->
                        // Calculate the neighboring cell id as an integer
                        val localCellId = int1Var(cellId + x + (y * gridCols))
                        `if`((localCellId lt 0.const) or (localCellId ge (gridCols * gridRows)) or (cellId + y * gridCols gt gridCols * gridRows)) {
                            `continue`()
                        }
                        val startIndex = int1Var(cellOffsets[localCellId])
                        fori(startIndex, count) { i ->
                            `if`(int1Var(particle2CellKey[i]) ne localCellId) { `break`() }
                            val otherPos = float3Var(currPositions[i].xyz)
//                            val otherVel = float2Var(currVelocities[i].xy)
                            `if`((otherPos.x eq position.x) and (otherPos.y eq position.y)) { `continue`() }
                            val direction = float3Var(position - otherPos)
                            val dist = float1Var(length(direction))
//                            `if`(dist gt gridSize) { `continue`() }
                            val forceBetweenParticles = float1Var(0f.const)
                            val otherType = int1Var(particleTypes[i])
                            // Compute a hash based on the particle types
                            val hash = int1Var((otherType xor particleType) shl 16.const) or (otherType or particleType)

                            // Prepare function calls for all pairwise interactions
                            val functionCalls: List<Pair<List<KslScalarExpression<KslFloat1>>, Int>> =
                                config.pairwiseInteraction.map { interaction ->
                                    interaction.functions.map { functionWithParams ->
                                        val kslFunc = functions[functionWithParams.function.name] as? KslFunctionFloat1
                                            ?: error("Function ${functionWithParams.function.name} not registered")
                                        kslFunc.invoke(
                                            dist,
                                            maxForce,
                                            *functionWithParams.parameters.map { (glsl, param) ->
                                                when (param) {
                                                    is Parameter.Value<*> -> (param.value as Float).const
                                                    is Parameter.FromParams<*> -> uniformFloat1("${glsl.name}_${functionWithParams.uniformPrefix}")
                                                }
                                            }.toTypedArray()
                                        )
                                    } to interaction.type.hash
                                }

                            // Add to pairwise force based on particle interaction hash
                            functionCalls.fold(`if`(false.const) {}) { acc, curr ->
                                acc.elseIf(hash eq curr.second.const) {
                                    forceBetweenParticles += curr.first.reduce { acc, curr -> acc + curr }
                                }
                            }

                            // (Insert your force calculation logic here which should modify forceBetweenParticles)
                            // Accumulate force (direction normalized multiplied by the calculated force)
                            netForce += normalize(direction) * forceBetweenParticles
                        }
                    }
                }

                // Cap velocity and net force to their maximum values
                `if`(length(velocity) gt maxVelocity) {
                    velocity set normalize(velocity) * maxVelocity
                }
                `if`(length(netForce) gt maxForce) {
                    netForce set normalize(netForce) * maxForce
                }
                // --- Begin wall repulsion snippet ---
                // Define simulation box boundaries
                val boxMin = float3Var(Vec3f.ZERO.const)
                val boxMax = float3Var(
                    float3Value(
                        gridSize * gridCols.toFloat1(),
                        gridSize * gridRows.toFloat1(),
                        gridSize * gridDepth.toFloat1()
                    )
                )

                // Wall repulsion
                fun lJ(dist: KslExpression<KslFloat1>) = (functions["lennardJones"] as KslFunctionFloat1)
                    .invoke(dist, maxForce, 2f.const, 10f.const)
                netForce.x += lJ(position.x - boxMin.x)
                netForce.x -= lJ(boxMax.x - position.x)
                netForce.y += lJ(position.y - boxMin.y)
                netForce.y -= lJ(boxMax.y - position.y)
                `if`(gridDepth ne 0.const) {
                    netForce.z += lJ(position.z - boxMin.z)
                    netForce.z -= lJ(boxMax.z - position.z)
                }

                // Compute next position with Verlet integration:
                // nextPosition = position + velocity * dT + (netForce * dT^2) / 2
//                val prevForce = float3Var(prevForces[id].xyz)
                // p(t + dT)
                val nextPosition = float3Var(position + (velocity * dT) + ((netForce * dT * dT) / 2f.const))
                // v(t + dT)
                val nextVelocity = float3Var(velocity + (netForce * dT / 2f.const))

                // Ensure particles are in bounds
                `if`(nextPosition.x.lt(0f.const)) { nextPosition.x set 1f.const }
                `if`(nextPosition.y.lt(0f.const)) { nextPosition.y set 1f.const }
                `if`(nextPosition.x.gt(boxMax.x)) { nextPosition.x set boxMax.x - 1f.const }
                `if`(nextPosition.y.gt(boxMax.y)) { nextPosition.y set boxMax.y - 1f.const }
                `if`(gridDepth ne 0.const) {
                    `if`(nextPosition.z.lt(0f.const)) { nextPosition.z set 1f.const }
                    `if`(nextPosition.z.gt(boxMax.z)) { nextPosition.z set boxMax.z - 1f.const }
                }

                prevPositions[id] = float4Value(nextPosition, 0f)
                prevVelocities[id] = float4Value(nextVelocity, 0f)
                prevForces[id] = float4Value(netForce, 0f)
                // Update the color buffer based on the magnitude of the net force
                colors[id] = float4Value(
                    log(length(netForce)) / (2f.const * log(1000f.const)),
                    0f.const, 0f.const, 1f.const
                )
//                colors[id] = float4Value(
//                    currPositions[id].y / (gridSize * gridRows.toFloat1()),
////                    id.toFloat1() / count.toFloat1(),
////                    log(length(netForce)) / (2f.const * log(maxForce)),
//                    currPositions[id].x / (gridSize * gridRows.toFloat1()),
//                    1f.const, 1f.const
//                )
            }
        }

    }

    // Uniforms
    var gridSize by shader.uniform1f("gridSize")
    var gridRows by shader.uniform1i("gridRows")
    var gridDepth by shader.uniform1i("gridDepth")
    var gridCols by shader.uniform1i("gridCols")
    var dT by shader.uniform1f("dT")
    var count by shader.uniform1i("count")
    var maxForce by shader.uniform1f("maxForce")
    var maxVelocity by shader.uniform1f("maxVelocity")
    //{{ uniforms }}

    // Storage buffers
    var particle2CellKey by shader.storage1d("particle2CellKey")
    var cellOffsets by shader.storage1d("cellOffsets")
    var currPositions by shader.storage1d("currPositions")
    var prevPositions by shader.storage1d("prevPositions")
    var currVelocities by shader.storage1d("currVelocities")
    var prevVelocities by shader.storage1d("prevVelocities")
    var prevForces by shader.storage1d("prevForces")
    var colors by shader.storage1d("colors")

    var particleTypes by shader.storage1d("particleTypes")
}
