//package template
//
//import de.fabmax.kool.modules.ksl.KslComputeShader
//import de.fabmax.kool.modules.ksl.lang.KslInt1
//import de.fabmax.kool.modules.ksl.lang.x
//
//val computeShader = KslComputeShader("Offset Compute") {
//    // Compute stage settings
//    computeStage(32) {
//        // Uniform variables
//        val numValues = uniformInt1("numValues")
//
//        // Storage buffers
//        val keysBuffer = storage1d<KslInt1>("keys")
//        val offsetsBuffer = storage1d<KslInt1>("offsets")
//
//        main {
//            val id = inGlobalInvocationId.x.toUInt()
//
//            // Early return if beyond numValues
//            `if` (id >= numValues) continue
//
//            val nullValue = numValues.toUInt()
//
//            // Get current key and previous key
//            val key = keysBuffer[id]
//            var keyPrev: UInt
//
//            if (id == 0u) {
//                keyPrev = nullValue
//            } else {
//                keyPrev = keysBuffer[id - 1u]
//            }
//
//            // Update offsets if keys differ
//            if (key != keyPrev) {
//                offsetsBuffer[key] = id
//            }
//        }
//    }
//}
