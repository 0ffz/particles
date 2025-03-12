package me.dvyy.particles.config

import de.fabmax.kool.pipeline.ComputeShader

data class UniformParameter<T: Any>(
    val name: String,
    val configPath: String,
    val uniformName: String,
    val value: T,
    val precision : Int = 0,
    val range: ClosedFloatingPointRange<Double> = 0.0..100.0,
)  {
    fun setUniform(shader: ComputeShader) = when(value) {
        is Float -> shader.uniform1f(uniformName).set(value)
        is Int -> shader.uniform1i(uniformName).set(value)
        else -> error("Can't set uniform of type ${value::class}")
    }
}
