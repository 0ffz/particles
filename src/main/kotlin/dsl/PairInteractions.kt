package dsl

//data class FunctionWithParameters(
//    val function: InteractionFunction,
////    val parameters: List<Any>,
//)
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
