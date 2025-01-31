package me.dvyy.particles.dsl

class ParticleInteractions(
    val types: List<ParticleType>
) {
    val pairInteractions = mutableListOf<PairInteractions>()

    inline operator fun Pair<ParticleType, ParticleType>.invoke(block: PairInteractions.() -> Unit) {
        PairInteractions(this).apply(block).also { pairInteractions.add(it) }
    }

    operator fun ParticleType.minus(other: ParticleType) = this to other

    inline fun allPairs(block: PairInteractions.() -> Unit) {
        for (i in types.indices) {
            for (j in i until types.size) {
                (types[i] - types[j])(block)
            }
        }
    }
}
