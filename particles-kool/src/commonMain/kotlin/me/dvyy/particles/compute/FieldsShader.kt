package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputePass
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.UniformParameters
import me.dvyy.particles.dsl.pairwise.ParticlePair

class FieldsShader(
    val configRepo: ConfigRepository,
    val buffers: ParticleBuffers,
    val uniforms: UniformParameters,
    val forcesDef: ForcesDefinition,
) {
    val halfStep = KslComputeShader("Fields Half-Step") {
        computeStage(WORK_GROUP_SIZE) {
            val dT = uniformFloat1("dT")
            val positions = storage1d<KslFloat4>("positions")
            val velocities = storage1d<KslFloat4>("velocities")
            val forces = storage1d<KslFloat4>("forces")
            val boxMax = uniformFloat3("boxMax")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val position = float3Var(positions[id].xyz) //p(t)
                val velocity = float3Var(velocities[id].xyz) //v(t)
                val force = float3Var(forces[id].xyz) //a(t)

                // Verlet integration with half-step eliminated p(t + dt) = p(t) + v(t)dT + 1/2 a(t)dT^2
                val nextPosition = float3Var(position + (velocity * dT) + ((force * dT * dT) / 2f.const))

                // Ensure particles are in bounds
                `if`(nextPosition.x.lt(0f.const)) { nextPosition.x set 1f.const }
                `if`(nextPosition.y.lt(0f.const)) { nextPosition.y set 1f.const }
                `if`(nextPosition.x.gt(boxMax.x)) { nextPosition.x set boxMax.x - 1f.const }
                `if`(nextPosition.y.gt(boxMax.y)) { nextPosition.y set boxMax.y - 1f.const }
                `if`(boxMax.z ne 0f.const) {
                    `if`(nextPosition.z.lt(0f.const)) { nextPosition.z set 1f.const }
                    `if`(nextPosition.z.gt(boxMax.z)) { nextPosition.z set boxMax.z - 1f.const }
                }

                positions[id] = float4Value(nextPosition, 0f.const)
            }
        }
    }
    var halfStep_dT by halfStep.uniform1f("dT")
    var halfStep_positions by halfStep.storage1d("positions")
    var halfStep_velocities by halfStep.storage1d("velocities")
    var halfStep_forces by halfStep.storage1d("forces")
    var halfStep_boxMax by halfStep.uniform3f("boxMax")

    val shader = KslComputeShader("Fields") {
        val config = configRepo.config.value
        computeStage(WORK_GROUP_SIZE) {
            // Uniforms
            val gridSize = uniformFloat1("gridSize")
            val gridCells = uniformInt3("gridCells")
            val dT = uniformFloat1("dT")
            val count = uniformInt1("count")
            val maxForce = uniformFloat1("maxForce")
            val maxVelocity = uniformFloat1("maxVelocity")
            val boxMax = uniformFloat3("boxMax")

            // Storage buffers
            val particle2CellKey = storage1d<KslInt1>("particle2CellKey")
            val cellOffsets = storage1d<KslInt1>("cellOffsets")
            val positions = storage1d<KslFloat4>("positions")
            val velocities = storage1d<KslFloat4>("velocities")
            val forces = storage1d<KslFloat4>("forces")

            val colors = storage1d<KslFloat4>("colors")
            val particleTypes = storage1d<KslInt1>("particleTypes")

            // Define all force functions
            forcesDef.forces.forEach {
                it.createFunction(this)
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

                // Compute grid indices based on the particle position
                val xGrid = int1Var((position.x / gridSize).toInt1())
                val yGrid = int1Var((position.y / gridSize).toInt1())
                val zGrid = int1Var((position.z / gridSize).toInt1())
                // Compute the base cell id (convert to int for bounds checking)
//                val cellId = int1Var(cellId(xGrid, yGrid, zGrid))


                // a(t + dT)
                val nextForce = float3Var(Vec3f.ZERO.const)

                // Loop over neighboring grid cells (x and y offsets from -1 to 1)
                fori((-1).const, 2.const) { x ->
                    fori((-1).const, 2.const) { y ->
                        fun forZIf3d(block: KslScopeBuilder.(KslScalarExpression<KslInt1>) -> Unit) {
                            if (config.simulation.threeDimensions) fori((-1).const, 2.const) { z ->
                                block(z)
                            } else block(0.const)
                        }
                        forZIf3d { z ->
                            // Calculate the neighboring cell id as an integer
                            `if`(
                                (xGrid lt 0.const) or (xGrid gt gridCells.x)
                                        or (yGrid lt 0.const) or (yGrid gt gridCells.y)
                                        or (zGrid lt 0.const) or (zGrid gt gridCells.z)
                            ) {
                                `continue`()
                            }
                            val localCellId = int1Var(cellId(xGrid + x, yGrid + y, zGrid + z))

                            val startIndex = int1Var(cellOffsets[localCellId])

                            // Individual forces
                            val individualForceInvocations =
                                forcesDef.individualInteractions.map { (particle, forces) ->
                                    val key = ParticlePair(particle, particle)
                                    val invocations = forces.map {
                                        val kslFunction = functions[it.function.name] as? KslFunctionFloat3
                                            ?: error("Function ${it.function.name} not registered")
                                        // TODO apply maxForce to individual forces
                                        kslFunction.invoke(
                                            position,
                                            *it.getParameters(this, this@KslComputeShader, key)
                                        )
                                    }
                                    invocations to key.hash
                                }

                            individualForceInvocations.fold(`if`(false.const) {}) { acc, (invocations, hash) ->
                                acc.elseIf(particleType eq hash.const) {
                                    // Sum all functions, add this to the net force
                                    nextForce += invocations.reduce { acc, curr -> acc + curr }
                                }
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
                                val particleHash =
                                    int1Var((otherType xor particleType) shl 16.const) or (otherType or particleType)

                                // Prepare function calls for all pairwise interactions
                                val functionCalls = forcesDef.pairwiseInteractions.map { (pair, forces) ->
                                    val invocations = forces.map {
                                        val kslFunction = functions[it.function.name] as? KslFunctionFloat1
                                            ?: error("Function ${it.function.name} not registered")
                                        kslFunction.invoke(
                                            dist,
                                            *it.getParameters(this, this@KslComputeShader, pair)
                                        )
                                    }
                                    invocations to pair.hash
                                }

                                // Add to pairwise force based on particle interaction hash
                                // TODO replace with a switch statement if added to KSL
                                functionCalls.fold(`if`(false.const) {}) { acc, (invocations, hash) ->
                                    acc.elseIf(particleHash eq hash.const) {
                                        // Sum all functions, add this to the net force
                                        forceBetweenParticles += min(invocations.reduce { acc, curr -> acc + curr }, maxForce)
                                    }
                                }

                                // (Insert your force calculation logic here which should modify forceBetweenParticles)
                                // Accumulate force (direction normalized multiplied by the calculated force)
                                nextForce += normalize(direction) * forceBetweenParticles
                            }
                        }
                    }
                }

                val velocity = float3Var(velocities[id].xyz) //v(t + dt/2)

                // --- Begin wall repulsion snippet ---
                // Define simulation box boundaries
                // Wall repulsion
                //TODO make configurable, since lennardJones might not be provided
                fun lJ(dist: KslExpression<KslFloat1>) = (functions["lennardJones"] as KslFunctionFloat1)
                    .invoke(dist, 2f.const, 1f.const)
                nextForce.x += lJ(position.x - 0f.const + 1f.const)
                nextForce.x -= lJ(boxMax.x - position.x + 1f.const)
                nextForce.y += lJ(position.y - 0f.const + 1f.const)
                nextForce.y -= lJ(boxMax.y - position.y + 1f.const)
                `if`(boxMax.z ne 0f.const) {
                    nextForce.z += lJ(position.z - 0f.const + 1f.const)
                    nextForce.z -= lJ(boxMax.z - position.z + 1f.const)
                }
                // Cap force
                `if`(length(nextForce) gt maxForce) {
                    nextForce set normalize(nextForce) * maxForce
                }

                // Compute next velocity with Verlet integration
                val currForce = float3Var(forces[id].xyz)
                val nextVelocity = float3Var(velocity + ((currForce + nextForce) * dT / 2f.const))
                // Cap velocity and net force to their maximum values
                `if`(length(nextVelocity) gt maxVelocity) {
                    nextVelocity set normalize(nextVelocity) * maxVelocity
                }

                forces[id] = float4Value(nextForce, 0f)
                velocities[id] = float4Value(nextVelocity, 0f)

                // Update the color buffer based on the magnitude of the net force
                colors[id] = float4Value(
                    log(length(nextForce)) / (2f.const * log(1000f.const)),
                    0f.const, 0f.const, 1f.const
                )
            }
        }

    }

    // Uniforms
    var gridSize by shader.uniform1f("gridSize")
    var gridCells by shader.uniform3i("gridCells")
    var dT by shader.uniform1f("dT")
    var count by shader.uniform1i("count")
    var maxForce by shader.uniform1f("maxForce")
    var maxVelocity by shader.uniform1f("maxVelocity")

    // Storage buffers
    var particle2CellKey by shader.storage1d("particle2CellKey")
    var cellOffsets by shader.storage1d("cellOffsets")
    var positions by shader.storage1d("positions")
    var velocities by shader.storage1d("velocities")
    var forces by shader.storage1d("forces")
    var boxMax by shader.uniform3f("boxMax")
    var colors by shader.storage1d("colors")

    var particleTypes by shader.storage1d("particleTypes")

    private fun initBuffers() {
        gridSize = configRepo.gridSize
        gridCells = configRepo.gridCells
        count = configRepo.count
        colors = buffers.colorsBuffer
        particleTypes = buffers.particleTypesBuffer
        cellOffsets = buffers.offsetsBuffer
        particle2CellKey = buffers.particleGridCellKeys
        positions = buffers.positionBuffer
        velocities = buffers.velocitiesBuffer
        forces = buffers.forcesBuffer
        boxMax = configRepo.boxSize

        halfStep_positions = buffers.positionBuffer
        halfStep_velocities = buffers.velocitiesBuffer
        halfStep_forces = buffers.forcesBuffer
        halfStep_boxMax = configRepo.boxSize
    }

    fun addTo(computePass: ComputePass) {
        val config = configRepo.config.value

        initBuffers()

        repeat(config.simulation.passesPerFrame) { passIndex ->
            computePass.addTask(halfStep, numGroups = configRepo.numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        halfStep_dT = simulation.dT.toFloat()
                        val count = simulation.targetCount
                        setNumGroupsByInvocations(count)
                    }
                }
            }
            computePass.addTask(shader, numGroups = configRepo.numGroups).apply {
                onBeforeDispatch {
                    configRepo.whenDirty {
                        dT = simulation.dT.toFloat()
                        maxVelocity = simulation.maxVelocity.toFloat()
                        maxForce = simulation.maxForce.toFloat()
                        val count = simulation.targetCount
                        this@FieldsShader.count = count
                        setNumGroupsByInvocations(count)
                    }
                    //TODO move up to whenDirty
                    uniforms.uniformParams.value.forEach { uniform ->
                        uniform.setUniform(shader)
                    }
                }
            }
        }
    }
}
