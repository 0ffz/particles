package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable

@Serializable
data class Size(
    val width: Int = 1000,
    val height: Int = 1000,
    val depth: Int = 1000,
)
