package template

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*

const val WORK_GROUP_SIZE = 64
//val ReindexShader = KslComputeShader("Reindex Elements") {
//    computeStage(WORK_GROUP_SIZE) {
//        val indices = storage1d<KslInt1>("indices")
//
//    }
//}
val GPUSort = KslComputeShader("GPUSort") {
    computeStage(WORK_GROUP_SIZE) {
//        val groupWidth = uniformInt1("groupWidth")
//        val groupHeight = uniformInt1("groupHeight")
//        val stepIndex = uniformInt1("stepIndex")

        val keys = storage1d<KslInt1>("keys")
        val indices = storage1d<KslInt1>("indices")

        val numValues = uniformInt1("numValues")
        val stage = uniformInt1("stage")
        val passOfStage = uniformInt1("passOfStage")

        main {
            // get global invocation id
            val idx = int1Var(inGlobalInvocationId.x.toInt1())

            // pairDistance = 1 << (stage - passOfStage)
            val diff = int1Var(stage - passOfStage)
            val pairDistance = int1Var(1.const shl diff)

            // blockWidth = 2 * pairDistance
            val blockWidth = int1Var(2.const * pairDistance)

            // leftId = (idx / pairDistance) * blockWidth + (idx rem pairDistance)
            val leftId = int1Var((idx / pairDistance) * blockWidth + idx.rem(pairDistance))
            // rightId = leftId + pairDistance
            val rightId = int1Var(leftId + pairDistance)

            `if`(rightId lt numValues) {
                // Determine sorting direction: ascending if ((idx / (1 << stage)) & 1) == 0
                val temp = int1Var(1.const shl stage)
                val ascending = bool1Var(((idx / temp) and 1.const) eq 0.const)

                // Load the two elements from the buffer
                val leftIndex = int1Var(indices[leftId])
                val rightIndex = int1Var(indices[rightId])
                val leftKey = int1Var(keys[leftId])
                val rightKey = int1Var(keys[rightId])

                // Decide whether to swap based on the comparison and the sort order
                val needSwap = bool1Var((leftKey gt rightKey) eq ascending)
                `if`(needSwap) {
                    indices[leftId] = rightIndex
                    indices[rightId] = leftIndex
                    keys[leftId] = rightKey
                    keys[rightId] = leftKey
                }
            }
        }
    }
}
