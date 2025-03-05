package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.lang.KslExpression
import de.fabmax.kool.modules.ksl.lang.KslInt3
import de.fabmax.kool.modules.ksl.lang.KslShaderStage
import de.fabmax.kool.modules.ksl.lang.functionInt1
import de.fabmax.kool.modules.ksl.lang.plus
import de.fabmax.kool.modules.ksl.lang.times
import de.fabmax.kool.modules.ksl.lang.x
import de.fabmax.kool.modules.ksl.lang.y

fun KslShaderStage.cellId(gridCells: KslExpression<KslInt3>) = functionInt1("cellId") {
    val xGrid = paramInt1("xGrid")
    val yGrid = paramInt1("yGrid")
    val zGrid = paramInt1("zGrid")
    body {
        xGrid + (yGrid * gridCells.x) + (zGrid * gridCells.x * gridCells.y)
    }
}
