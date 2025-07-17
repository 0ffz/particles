package me.dvyy.particles.config

data class UniformParameter(
    val name: String,
    val configPath: String,
    val value: Float,
    val precision: Int = when(value) {
        in 0f..1f -> 3
        in 0f..100f -> 2
        in 0f..10000f -> 1
        else -> 0
    },
    val range: ClosedFloatingPointRange<Double> = 0.0..100.0,
)
