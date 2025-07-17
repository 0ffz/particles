package me.dvyy.particles.compute.forces

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.ComputeShader
import me.dvyy.particles.compute.helpers.KslFloat
import me.dvyy.particles.dsl.pairwise.ParticlePair


class ForceWithParameters<T : Force>(
    val force: T,
    private val totalParticles: Int,
) {
    val hashCount = when(force) {
        is PairwiseForce -> totalParticles * totalParticles
        is IndividualForce -> totalParticles
        else -> error("Invalid force type")
    }

    private val uniformName = "${force.name}_parameters"
    private val parameterMatrices = mutableMapOf<Int, Mat4f>()
    private val numParameters = force.parameters.size

    fun put(pair: ParticlePair, values: FloatArray) {
        put(pair.hash, values)
    }

    fun put(hash: Int, values: FloatArray) {
        parameterMatrices[hash] = toStorage(values)
    }


    fun get(pair: ParticlePair): Mat4f? = parameterMatrices[pair.hash]

    /** Creates a UBO representing the parameters */
    context(program: KslProgram)
    val kslUniformBuffer get() = program.uniformMat4Array(uniformName, hashCount)

    context(program: KslProgram, scope: KslScopeBuilder)
    fun extractParameters(mat: KslVarMatrix<KslMat4, KslFloat4>): Array<KslFloat> = with(scope) {
        Array(numParameters) { i -> mat[(i + 1) / 4][(i + 1) % 4] }
    }

    /** Updates bound UBO with parameters on CPU. */
    context(shader: ComputeShader)
    fun uploadParameters() {
        val ubo = shader.uniformMat4fv(uniformName, hashCount)
        // Clear all data (zero matrix represents skipping parameters)
        repeat(hashCount) { i ->
            ubo[i] = Mat4f.ZERO
        }
        parameterMatrices.forEach { (hash, params) ->
            ubo[hash] = params
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
}

internal operator fun KslMatrix4Accessor.get(index: Int) = when(index) {
    0 -> x
    1 -> y
    2 -> z
    3 -> w
    else -> error("Invalid index $index for matrix")
}
