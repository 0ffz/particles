package me.dvyy.particles.compute.helpers

import de.fabmax.kool.modules.ksl.lang.*

fun KslShaderStage.cellId(gridCells: KslExpression<KslInt3>) = functionInt1("cellId") {
    val xGrid = paramInt1("xGrid")
    val yGrid = paramInt1("yGrid")
    val zGrid = paramInt1("zGrid")
    body {
        xGrid + (yGrid * gridCells.x) + (zGrid * gridCells.x * gridCells.y)
    }
}
