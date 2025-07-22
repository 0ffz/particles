package me.dvyy.particles.compute.simulation

import de.fabmax.kool.math.PI_F
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.ParticleBuffers
import me.dvyy.particles.compute.forces.ForcesDefinition
import me.dvyy.particles.compute.forces.PairwiseForce
import me.dvyy.particles.compute.helpers.KslFloat
import me.dvyy.particles.compute.helpers.cellId
import me.dvyy.particles.compute.helpers.forNearbyGridCells
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.config.ConfigRepository

class FieldsShader(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
    val forcesDef: ForcesDefinition,
) {
    val shader = KslComputeShader("Fields") {
        computeStage(WORK_GROUP_SIZE) {
            // Uniforms
            val gridSize = uniformFloat1("gridSize")
            val gridCells = uniformInt3("gridCells")
            val dT = uniformFloat1("dT")
            val count = uniformInt1("count")
            val params = uniformStruct("params", provider = ::SimulationParametersStruct)
            val boxMax = uniformFloat3("boxMax")

            // Storage buffers
            val particle2CellKey = storage<KslInt1>("particle2CellKey")
            val cellOffsets = storage<KslInt1>("cellOffsets")
            val positions = storage<KslFloat4>("positions")
            val velocities = storage<KslFloat4>("velocities")
            val forces = storage<KslFloat4>("forces")

            val particleTypes = storage<KslInt1>("particleTypes")

            // Define all force functions, create uniforms for their parameters
            forcesDef.forceTypes.forEach {
                it.createFunction()
            }
            forcesDef.forces.forEach {
                it.kslForcesStruct
            }

            // Helper: compute cell id from grid coordinates (cell id = x + y * gridCols)
            val cellId = cellId(gridCells)

            main {
                // Get the particle id from the global invocation (using only x as in GLSL)
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                // Load current particle properties
                // Extract the 2D position from the stored vec4
                val position = float3Var(positions[id].xyz) //p(t + dt); since half step runs before this
                val particleType = int1Var(particleTypes[id])
                val params = params.struct

                // Compute grid indices based on the particle position
                val grid = int3Var((position / gridSize).toInt3())

                // a(t + dT)
                val nextForce = float3Var(Vec3f.ZERO.const)

                // Loop over neighboring grid cells (x and y offsets from -1 to 1)
                forNearbyGridCells(configRepo.config.value.simulation.threeDimensions) { x, y, z ->
                    `if`(any(grid + int3Value(x, y ,z) lt 0.const3) or any(grid + int3Value(x, y ,z) gt gridCells)) {
                        `continue`()
                    }
                    // Calculate the neighboring cell id as an integer
                    val localCellId = int1Var(cellId(grid.x + x, grid.y + y, grid.z + z))
                    val startIndex = int1Var(cellOffsets[localCellId])

                    fun KslFloat.clampMaxForce() = min(this, params.maxForce.ksl)

                    // Individual forces
                    // TODO apply maxForce to individual forces
                    forcesDef.individualForces.forEach {
                        val functionRef = it.force.kslReference
                        val interaction = structVar(it.interactionFor(particleType)).struct
                        nextForce += interaction.enabled.ksl * functionRef.invoke(
                            position,
                            *interaction.parametersAsArray()
                        )
                    }

                    // TODO move into preprocess step for pairwise forces
                    // Calculate local neighbours (as in tersoff)
                    // Given distance between two particles, return a smoothed cutoff from 1 to 0
                    val localCount = float1Var(0f.const)

                    fun cutoff(
                        distance: KslScalarExpression<KslFloat1>,
                        cutoffR: KslScalarExpression<KslFloat1>,
                        cutoffD: KslScalarExpression<KslFloat1>,
                    ): KslScalarExpression<KslFloat1> {
                        // Clamp the distance to the transition range [lowerBound, upperBound].
                        val clampedDistance = clamp(distance, cutoffR - cutoffD, cutoffR + cutoffD)
                        return 0.5f.const - (0.5f.const * sin((PI_F / 2f).const * (clampedDistance - cutoffR) / cutoffD))
                    }

                    //TODO duplicate code
                    fori(startIndex, count) { i ->
                        `if`(int1Var(particle2CellKey[i]) ne localCellId) { `break`() }
                        val otherPos = float3Var(positions[i].xyz)
                        `if`((otherPos.x eq position.x) and (otherPos.y eq position.y) and (otherPos.z eq position.z)) { `continue`() }
                        val direction = float3Var(position - otherPos)
                        val dist = float1Var(length(direction))
                        `if`(dist gt 5f.const) { `continue`() }
                        localCount += cutoff(dist, 0.3f.const, 5f.const) //TODO cutoff function
                    }

                    // Pairwise forces
                    fori(startIndex, count) { i ->
                        `if`(int1Var(particle2CellKey[i]) ne localCellId) { `break`() }
                        val otherPos = float3Var(positions[i].xyz)
                        `if`((otherPos.x eq position.x) and (otherPos.y eq position.y) and (otherPos.z eq position.z)) { `continue`() }
                        val direction = float3Var(position - otherPos)
                        val dist = float1Var(length(direction))
                        `if`(dist gt gridSize) { `continue`() }
                        val forceBetweenParticles = float1Var(0f.const)
                        val otherType = int1Var(particleTypes[i])

                        // Compute a hash based on the particle types
                        val pairHash = PairwiseForce.pairHash(particleType, otherType, forcesDef.particleTypeCount)

                        // Call invoke each pairwise force function with extracted parameters
                        forcesDef.pairwiseForces.forEach {
                            val functionRef = it.force.kslReference
                            val interaction = structVar(it.interactionFor(pairHash)).struct
                            // For pairs without an interaction paramsMat[0][0] is 0
                            forceBetweenParticles += interaction.enabled.ksl *
                                    functionRef.invoke(dist, localCount, *interaction.parametersAsArray())
                                        .clampMaxForce()
                        }

                        nextForce += normalize(direction) * forceBetweenParticles
                    }
                }

                val velocity = float3Var(velocities[id].xyz) //v(t + dt/2)

                // --- Begin wall repulsion snippet ---
                // Define simulation box boundaries
                // Wall repulsion
                //TODO make configurable, since lennardJones might not be provided
//                fun lJ(dist: KslExpression<KslFloat1>) = (functions["lennardJones"] as KslFunctionFloat1)
//                    .invoke(dist, 1f.const, 5f.const, 0.2f.const)
//                nextForce.x += lJ(position.x - 0f.const + 1f.const)
//                nextForce.x -= lJ(boxMax.x - position.x + 1f.const)
//                nextForce.y += lJ(position.y - 0f.const + 1f.const)
//                nextForce.y -= lJ(boxMax.y - position.y + 1f.const)
//                `if`(boxMax.z ne 0f.const) {
//                    nextForce.z += lJ(position.z - 0f.const + 1f.const)
//                    nextForce.z -= lJ(boxMax.z - position.z + 1f.const)
//                }
                // Cap force
                `if`(length(nextForce) gt params.maxForce.ksl) {
                    nextForce set normalize(nextForce) * params.maxForce.ksl
                }

                // Compute next velocity with Verlet integration
                val currForce = float3Var(forces[id].xyz)
                val nextVelocity = float3Var(velocity + ((currForce + nextForce) * dT / 2f.const))
                // Cap velocity and net force to their maximum values
                `if`(length(nextVelocity) gt params.maxVelocity.ksl) {
                    nextVelocity set normalize(nextVelocity) * params.maxVelocity.ksl
                }

                forces[id] = float4Value(nextForce, 0f)
                velocities[id] = float4Value(nextVelocity, 0f)
            }
        }

    }

    // Uniforms
    var gridSize by shader.uniform1f("gridSize")
    var gridCells by shader.uniform3i("gridCells")
    var dT by shader.uniform1f("dT")
    var count by shader.uniform1i("count")
    var boxMax by shader.uniform3f("boxMax")
    var params = shader.uniformStruct("params", ::SimulationParametersStruct)

    // Storage buffers
    var particle2CellKey by shader.storage("particle2CellKey")
    var cellOffsets by shader.storage("cellOffsets")
    var positions by shader.storage("positions")
    var velocities by shader.storage("velocities")
    var forces by shader.storage("forces")
    var particleTypes by shader.storage("particleTypes")
}
