package me.dvyy.particles.dsl

class PairInteractions(
    val type: Pair<ParticleType, ParticleType>,
) {
    /** A name for this pair, formatted as `firstName-secondName` */
    val pairKey = "${type.first.name}-${type.second.name}"
    val hashTop: UInt = type.first.id xor type.second.id shl 16 or type.second.id or type.first.id

    @OptIn(ExperimentalStdlibApi::class)
    val hash = hashTop.toHexString()
    val functions = mutableListOf<FunctionWithParameters>()


    operator fun <T : PairwiseFunction> T.invoke(configure: FunctionWithParameters.(T) -> Unit) {
        val setValues = FunctionWithParameters(this).apply { configure(this@invoke) }
        functions.add(setValues)
    }
}
