package me.dvyy.particles.compute.helpers

import de.fabmax.kool.modules.ksl.lang.KslScopeBuilder

/** Loops over a third z value if config is set to threeDimensions */
internal fun KslScopeBuilder.forZIf3d(
    isThreeDimensions: Boolean,
    block: KslScopeBuilder.(KslInt) -> Unit
) {
    if (isThreeDimensions) fori((-1).const, 2.const) { z ->
        block(z)
    } else block(0.const)
}

/**
 * Loops -1..1 in three dimensions (leaving the third as 0 if [isThreeDimensions] is false.)
 */
internal fun KslScopeBuilder.forNearbyGridCells(
    isThreeDimensions: Boolean,
    block: KslScopeBuilder.(x: KslInt, y: KslInt, z: KslInt) -> Unit
) {
    fori((-1).const, 2.const) { x ->
        fori((-1).const, 2.const) { y ->
            forZIf3d(isThreeDimensions) { z -> // A third loop if set to 3-dimensions
                block(x, y, z)
            }
        }
    }
}
