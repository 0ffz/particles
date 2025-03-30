package me.dvyy.particles.render

import de.fabmax.kool.modules.ui2.Sizes

enum class ParticleColor {
    TYPE, FORCE, CLUSTER
}

enum class UiScale(val size: Sizes) {
    SMALL(Sizes.small), MEDIUM(Sizes.medium), LARGE(Sizes.large);
}
