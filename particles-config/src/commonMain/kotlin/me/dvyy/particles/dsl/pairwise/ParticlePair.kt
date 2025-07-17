package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.ParticleId
import kotlin.math.max
import kotlin.math.min

@ConsistentCopyVisibility
data class ParticlePair private constructor(
    val first: ParticleId,
    val second: ParticleId,
    val hash: Int,
) {
    companion object {
        fun hashFor(first: ParticleId, second: ParticleId, totalCount: Int): Int {
            return min(first.id, second.id) + max(first.id, second.id) * totalCount
        }

        fun fromString(pair: String, particleIds: Map<String, ParticleId>, totalCount: Int): ParticlePair? {
            if (!pair.contains("-")) return null
            val (first, second) = pair.split("-")
            val firstId = particleIds[first] ?: return null
            val secondId = particleIds[second] ?: return null
            return of(firstId, secondId, totalCount)
        }

        fun of(first: ParticleId, second: ParticleId, totalCount: Int): ParticlePair {
            return ParticlePair(first, second, hashFor(first, second, totalCount))
        }
    }
}
