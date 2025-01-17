package dsl

class ParticleInteractions(
    val type: Pair<ParticleType, ParticleType>,
) {
    val hashTop: UInt = type.first.id xor type.second.id shl 16 or type.second.id or type.first.id

    @OptIn(ExperimentalStdlibApi::class)
    val hash = hashTop.toHexString()
    val functions = mutableListOf<InteractionFunction>()

    fun uses(function: InteractionFunction) {
        functions.add(function)
    }
}
