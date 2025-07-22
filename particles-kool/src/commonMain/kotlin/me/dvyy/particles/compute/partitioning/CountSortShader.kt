package me.dvyy.particles.compute.partitioning

import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*

class CountSortShader {
    val NUM_PER_WORK_ITEM = 64
    val workItems get() = cellIds!!.size / NUM_PER_WORK_ITEM + 1 //TODO verify
    // TODO assumes just 2 bit numbers for now
    val shader = KslComputeShader("Count Sort") {
//        val inputValues = storage<KslFloat4>("currPositions")
        val cellIds = storage<KslInt1>("cellIds")
//        val maxCellId = uniformInt1("maxCellId")
        val counts = storage<KslInt1>("counts")
        computeStage(WORK_GROUP_SIZE) {
            main {
                // What index to start reading inputs from
                val startIndex = int1Var((inGlobalInvocationId.x * NUM_PER_WORK_ITEM.toUInt().const).toInt1())
                // What index to start writing counts at
                val countStart = int1Var((inGlobalInvocationId.x * 4u.const).toInt1())
                repeat(NUM_PER_WORK_ITEM.const) { i ->
                    val bits = int1Var(cellIds[startIndex + i]) //TODO extract bits
                    counts[countStart + bits] = counts[countStart + bits] + 1.const
                }
            }
        }
    }

    var cellIds by shader.storage("cellIds")
    var counts by shader.storage("counts")

//    fun initialize() {
//        counts = cellIds.size * 4
//    }
//    val combineCounts = KslShader("Combine Counts") {
//        val maxCellId = uniformInt1("maxCellId")
//        val wgCounts = storage<KslInt1>("counts")
//        val combinedCounts = storage<KslInt1>("counts")
//        computeStage() {
//            main {
//                repeat(maxCellId) { i ->
//                    val sum = int1Var(0.const)
//
//                    combinedCounts[i] = sum
//                }
//            }
//        }
//    }
}
