package me.dvyy.particles.config

import kotlinx.serialization.Serializable

@Serializable
data class ClusterOptions(
    val enabled: Boolean,
    val radius: Double = 15.0,
    val minPoints: Int = 5,
    val drawGraph: Boolean = true,
)
