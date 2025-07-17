package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.ParticleId
import me.dvyy.particles.dsl.ParticlesConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Represents an unordered set of particle types (currently only sizes of 1 and 2 are supported).
 */
@ConsistentCopyVisibility
data class ParticleSet private constructor(
    val ids: List<ParticleId>,
    val hash: Int,
) {
    val size = ids.size

    companion object {
        fun hashFor(vararg particles: ParticleId, totalCount: Int): Int = when (particles.size) {
            1 -> particles[0].id
            2 -> min(particles[0].id, particles[1].id) + max(particles[0].id, particles[1].id) * totalCount
            else -> TODO("Arbitrary particle set hashes not yet implemented")
        }

        context(config: ParticlesConfig)
        fun fromString(set: String, count: Int? = null): ParticleSet {
            val names = set.split("-")
            if (count != null && names.size != count) error("Expected $count particles in set '$set', got ${names.size}")
            val ids = names.map { config.particle(it) }
            return of(*ids.toTypedArray())
        }

        context(config: ParticlesConfig)
        fun of(vararg particles: ParticleId): ParticleSet {
            return ParticleSet(particles.sortedBy { it.id }, hashFor(*particles, totalCount = config.particleTypeCount))
        }
    }
}
