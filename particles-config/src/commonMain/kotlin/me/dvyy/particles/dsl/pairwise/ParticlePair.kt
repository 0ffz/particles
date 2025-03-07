package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.ParticleId

data class ParticlePair(
    val first: ParticleId,
    val second: ParticleId,
) {
    val hash: Int = (first.id xor second.id shl 16 or second.id or first.id).toInt()

    companion object {
        fun fromString(pair: String, particleIds: Map<String, ParticleId>): ParticlePair {
            val (first, second) = pair.split("-")
            return ParticlePair(particleIds[first]!!, particleIds[second]!!)
        }
    }
}
