package me.dvyy.particles.render

import de.fabmax.kool.modules.ui2.Sizes
import kotlinx.serialization.Serializable

@Serializable
enum class ParticleColor {
    TYPE, FORCE, CLUSTER
}

@Serializable
enum class UiScale(val size: Sizes) {
    SMALL(Sizes.small), MEDIUM(Sizes.medium), LARGE(Sizes.large);
}
