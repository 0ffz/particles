package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable

@Serializable
data class Particle(
    val color: String,
    val radius: Double = 5.0,
    val distribution: Double = 1.0,
)
