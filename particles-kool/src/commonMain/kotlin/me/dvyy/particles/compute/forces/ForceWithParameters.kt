package me.dvyy.particles.compute.forces

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputeShader
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.dvyy.particles.compute.helpers.KslFloat
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
    private val parameterMatrices = mutableMapOf<ParticleSet, Mat4f>()
    val parameterNames = force.parameters.map { it.name }
    private val numParameters = force.parameters.size

    private val _changes = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }
    val changes = _changes.asSharedFlow()

    fun put(set: ParticleSet, values: FloatArray) {
        parameterMatrices[set] = toStorage(values)
        _changes.tryEmit(Unit)
    }

    fun update(set: ParticleSet, param: String, value: Float) {
        put(set, toArray(get(set)!!, numParameters).apply { set(parameterNames.indexOf(param), value) })
    }

    fun get(pair: ParticleSet): Mat4f? = parameterMatrices[pair]

    fun getAll(): Map<ParticleSet, FloatArray> = parameterMatrices.mapValues { (_, mat) ->
        toArray(mat, numParameters)
    }

    /** Creates a UBO representing the parameters */
    context(program: KslProgram)
    val kslUniformBuffer get() = program.uniformMat4Array(uniformName, hashCount)


    /** Updates bound UBO with parameters on CPU. */
    context(shader: ComputeShader)
    fun uploadParameters() {
        val ubo = shader.uniformMat4fv(uniformName, hashCount)
        // Clear all data (zero matrix represents skipping parameters)
        repeat(hashCount) { i ->
            ubo[i] = Mat4f.ZERO
        }
        parameterMatrices.forEach { (set, params) ->
            ubo[set.hash] = params
        }
    }

    //TODO update to use a struct once kool supports uniform struct arrays
    /**
     * Encodes given values into a 4x4 matrix to be used on the gpu
     *
     * The first byte is 0f for hashes that should be ignored,
     * 1f if a calculation should occur followed by its parameters, then zeroes.
     */
    private fun toStorage(values: FloatArray): Mat4f {
        val matrix = MutableMat4f()
        val encoded = FloatArray(16) { 0f }
        encoded[0] = 1f
        values.forEachIndexed { i, value -> encoded[i + 1] = value }
        matrix.set(encoded)
        return matrix
    }


    context(program: KslProgram, scope: KslScopeBuilder)
    fun extractParameters(mat: KslVarMatrix<KslMat4, KslFloat4>): Array<KslFloat> = with(scope) {
        Array(numParameters) { i -> mat[(i + 1) / 4][(i + 1) % 4] }
    }

    companion object {
        fun toArray(mat: Mat4f, numParameters: Int): FloatArray {
            return FloatArray(numParameters) { i -> mat[(i + 1) % 4, (i + 1) / 4] }
        }
    }
}

internal operator fun KslMatrix4Accessor.get(index: Int) = when (index) {
    0 -> x
    1 -> y
    2 -> z
    3 -> w
    else -> error("Invalid index $index for matrix")
}
