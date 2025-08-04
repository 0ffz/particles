package me.dvyy.particles.render

import de.fabmax.kool.modules.ui2.Sizes
import kotlinx.serialization.Serializable

@Serializable
enum class ParticleColor {
    TYPE, VELOCITY, FORCE, CLUSTER, NEIGHBOURS
}

@Serializable
enum class UiScale(val size: Sizes) {
    SMALL(Sizes.small), MEDIUM(Sizes.medium), LARGE(Sizes.large);
}
