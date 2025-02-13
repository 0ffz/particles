package me.dvyy.particles.dsl

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationConfiguration(
    var fullscreen: Boolean = false,
    var width: Int = 1280,
    var height: Int = 720,
)
