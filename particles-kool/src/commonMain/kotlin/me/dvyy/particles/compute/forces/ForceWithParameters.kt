package me.dvyy.particles.compute.forces

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputeShader
import de.fabmax.kool.util.MemoryLayout
import de.fabmax.kool.util.Struct
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.dvyy.particles.compute.helpers.KslInt
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.dsl.pairwise.ParticleSet


class ForceWithParameters<T : Force>(
    val force: T,
    private val totalParticles: Int,
) {
    val hashCount = when (force) {
        is PairwiseForce -> totalParticles * totalParticles
        is IndividualForce -> totalParticles
        else -> error("Invalid force type")
    }

    private val uniformName = "${force.name}_parameters"
    private val parameterMatrices = mutableMapOf<ParticleSet, FloatArray>()
    val parameterNames = force.parameters.map { it.name }
    private val numParameters = force.parameters.size

    private val _changes = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }
    val changes = _changes.asSharedFlow()

    fun put(set: ParticleSet, values: FloatArray) {
        parameterMatrices[set] = values
        _changes.tryEmit(Unit)
    }

    fun update(set: ParticleSet, param: String, value: Float) {
        put(set, get(set)!!.apply { set(parameterNames.indexOf(param), value) })
    }

    fun get(pair: ParticleSet): FloatArray? = parameterMatrices[pair]

    fun getAll(): Map<ParticleSet, FloatArray> = parameterMatrices

    /** Creates a UBO representing the parameters */
    context(program: KslProgram)
    val kslForcesStruct get() = program.uniformStruct(uniformName, provider = ::ForceParametersStruct)

    context(scope: KslScopeBuilder, program: KslProgram)
    fun interactionFor(hash: KslInt) =
        kslForcesStruct.struct.interactions.ksl[hash]

    /** Updates bound UBO with parameters on CPU. */
    context(shader: ComputeShader)
    fun uploadParameters() {
        val ubo = shader.uniformStruct(uniformName, ::ForceParametersStruct)
        // Clear all data (zero matrix represents skipping parameters)
        ubo.set {
            repeat(interactions.arraySize) { i ->
                interactions[i].enabled.set(0f)
                interactions[i].parameters.forEach { it.set(0f) }
            }
            parameterMatrices.forEach { (set, params) ->
                interactions[set.hash].enabled.set(1f)
                interactions[set.hash].parameters.forEachIndexed { i, param ->
                    param.set(params[i])
                }
            }
        }
    }
//
//    //TODO update to use a struct once kool supports uniform struct arrays
//    /**
//     * Encodes given values into a 4x4 matrix to be used on the gpu
//     *
//     * The first byte is 0f for hashes that should be ignored,
//     * 1f if a calculation should occur followed by its parameters, then zeroes.
//     */
//    private fun toStorage(values: FloatArray): Mat4f {
//        val matrix = MutableMat4f()
//        val encoded = FloatArray(16) { 0f }
//        encoded[0] = 1f
//        values.forEachIndexed { i, value -> encoded[i + 1] = value }
//        matrix.set(encoded)
//        return matrix
//    }
//
//
//    context(program: KslProgram, scope: KslScopeBuilder)
//    fun extractParameters(mat: KslVarStruct<ForceParametersStruct>): Array<KslFloat> = with(scope) {
//        Array(numParameters) { i -> mat[(i + 1) / 4][(i + 1) % 4] }
//    }

    fun createPairwiseForceComputeShader() = KslComputeShader("force-one-shot") {
        computeStage(WORK_GROUP_SIZE) {
            val localNeighbors = uniformFloat1("localNeighbors")
            val lastIndex = uniformInt1("lastIndex")
            val distances = storage<KslFloat1>("distances")
            val output = storage<KslFloat1>("outputBuffer")
            kslForcesStruct

            val function = (force as PairwiseForce).createFunction()
            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                `if`(id le lastIndex) {
                    val extractedParams = structVar(interactionFor(0.const)).struct
                    output[id] = function.function.invoke(distances[id], localNeighbors, *extractedParams.parametersAsArray())
                }
            }
        }
    }

    inner class InteractionStruct() : Struct("InteractionStruct_${force.name}", MemoryLayout.Std140) {
        val enabled = float1("enabled")
        val parameters = (0..<numParameters).map { float1() }

        fun parametersAsArray() = parameters.map { it.ksl }.toTypedArray()
    }

    inner class ForceParametersStruct() : Struct("ForceParametersStruct_${force.name}", MemoryLayout.Std140) {
        val interactions = structArray(hashCount, "interactions", structProvider = ::InteractionStruct)
    }
}

internal operator fun KslMatrix4Accessor.get(index: Int) = when (index) {
    0 -> x
    1 -> y
    2 -> z
    3 -> w
    else -> error("Invalid index $index for matrix")
}
