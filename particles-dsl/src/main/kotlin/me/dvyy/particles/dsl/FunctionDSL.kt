package me.dvyy.particles.dsl

import org.openrndr.color.ColorRGBa

class ParticleType(
    val name: String,
    val color: ColorRGBa,
    val radius: Double,
    val distribution: Double,
    val id: UInt,
)
