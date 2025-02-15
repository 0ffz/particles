package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*

class FieldsShader {
    val shader = KslComputeShader("Fields") {
        computeStage(WORK_GROUP_SIZE) {
            // Uniforms
            val gridSize = uniformFloat1("gridSize")
            val gridRows = uniformInt1("gridRows")
            val gridCols = uniformInt1("gridCols")
            val epsilon = uniformFloat1("epsilon")
            val sigma = uniformFloat1("sigma")
            val dT = uniformFloat1("dT")
            val count = uniformInt1("count")
            val maxForce = uniformFloat1("maxForce")
            val maxVelocity = uniformFloat1("maxVelocity")
            //{{ uniforms }}

            // Storage buffers
            val particle2CellKey = storage1d<KslInt1>("particle2CellKey")
            // (binding = 1 is omitted, as in the GLSL code)
            val cellOffsets = storage1d<KslInt1>("cellOffsets")
            val indexLookup = storage1d<KslInt1>("indexLookup")
            val currPositions = storage1d<KslFloat4>("currPositions")
            val prevPositions = storage1d<KslFloat4>("prevPositions")
            val currVelocities = storage1d<KslFloat4>("currVelocities")
            val prevVelocities = storage1d<KslFloat4>("prevVelocities")

            val colors = storage1d<KslFloat4>("colors")
            val particleTypes = storage1d<KslInt1>("particleTypes")

            //{{ forceFunctions }}

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
                val position = float2Var(currPositions[id].xy)
                val velocity = float2Var(currVelocities[id].xy)
//                val type = int1Var(particleTypes[id])

                // Compute grid indices based on the particle position
                val xGrid = int1Var((position.x / gridSize).toInt1())
                val yGrid = int1Var((position.y / gridSize).toInt1())
                // Compute the base cell id (convert to int for bounds checking)
                val cellId = int1Var(cellId(xGrid, yGrid, gridCols))

                val netForce = float2Var(float2Value(0f.const, 0f.const))

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
                            val otherPos = float2Var(currPositions[i].xy)
//                            val otherVel = float2Var(currVelocities[i].xy)
                            `if`((otherPos.x eq position.x) and (otherPos.y eq position.y)) { `continue`() }
                            val direction = float2Var(position - otherPos)
                            val dist = float1Var(length(direction))
                            `if`(dist gt gridSize) { `continue`() }
                            val forceBetweenParticles = float1Var(0f.const)
                            val otherType = int1Var(particleTypes[i])
                            // Compute a hash based on the particle types
//                        val hash = ((otherType xor type) shl 16.const) or (otherType or type)

//                        float inv_r = sigma / dist;
//                        float inv_r6 = inv_r * inv_r * inv_r * inv_r * inv_r * inv_r;
//                        float inv_r12 = inv_r6 * inv_r6;
//                        return min(24.0 * epsilon * (2.0 * inv_r12 - inv_r6) / dist, maxForce);
                            val invR = float1Var(sigma / dist)
                            val invR6 = float1Var(invR * invR * invR * invR * invR * invR)
                            val invR12 = float1Var(invR6 * invR6)
                            val lennardJonesForce =
                                float1Var(min(24f.const * epsilon * (2f.const * invR12 * invR6) / dist, maxForce))

                            // TODO switch {{ forceCalculations }}

                            forceBetweenParticles += lennardJonesForce
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

                // Compute next position with Verlet integration:
                // nextPosition = position + velocity * dT + (netForce * dT^2) / 2
                val nextPosition = float2Var(position + velocity * dT)// + ((netForce * dT * dT) / 2f.const))

//                nextPosition.x set nextPosition.x % gridCols.toFloat1() * gridSize
//                nextPosition.y set nextPosition.y % gridRows.toFloat1() * gridSize
                // Write back to the previous-particles buffer
                // Here we use mod() to wrap positions inside the grid dimensions
                `if`(nextPosition.x.lt(0f.const)) { nextPosition.x set gridSize * gridCols.toFloat1() - 1f.const  }
                `if`(nextPosition.y.lt(0f.const)) { nextPosition.y set gridSize * gridRows.toFloat1() - 1f.const }
                `if`(nextPosition.x.gt(gridSize * gridCols.toFloat1())) { nextPosition.x set 1f.const }
                `if`(nextPosition.y.gt(gridSize * gridRows.toFloat1())) { nextPosition.y set 1f.const }

                prevPositions[id] = float4Value(nextPosition, 0f, 0f)
                prevVelocities[id] = float4Value(velocity + netForce / 2f.const * dT, 0f, 0f)
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
    var gridCols by shader.uniform1i("gridCols")
    var epsilon by shader.uniform1f("epsilon")
    var sigma by shader.uniform1f("sigma")
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
    var colors by shader.storage1d("colors")

    var particleTypes by shader.storage1d("particleTypes")
}
