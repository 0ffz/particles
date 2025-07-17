package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable

@Serializable
data class Particle(
    val color: String = "ffffff",
    val radius: Double = 5.0,
    val distribution: Double = 1.0,
    val convertTo: Conversion? = null
)

@Serializable
data class Conversion(
    val type: String,
    val chance: Double,
)
