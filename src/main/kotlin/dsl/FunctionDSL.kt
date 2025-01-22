package dsl

import org.openrndr.color.ColorRGBa

data class ParticleType(
    val name: String,
    val color: ColorRGBa,
    val radius: Double,
    val id: UInt,
)

