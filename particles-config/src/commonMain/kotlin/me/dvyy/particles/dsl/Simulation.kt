package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable

@Serializable
data class Simulation(
    val count: Int = 10_000,
    val conversionRate: Int = 100,
    val minGridSize: Double = 5.0,
    val dT: Double = 0.001,
    val maxVelocity: Double = 20.0,
    val maxForce: Double = 100000.0,
    val threeDimensions: Boolean = false,
    val passesPerFrame: Int = 100,
    val size: Size = Size(),
)
