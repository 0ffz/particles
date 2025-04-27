package me.dvyy.particles.config

import kotlinx.serialization.Serializable

@Serializable
data class ClusterOptions(
    val enabled: Boolean,
    val radius: Double = 10.0,
    val minPoints: Int = 10,
    val drawGraph: Boolean = true,
)
