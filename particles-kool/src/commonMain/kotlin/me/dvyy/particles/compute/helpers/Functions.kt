package me.dvyy.particles.compute.helpers

import de.fabmax.kool.modules.ksl.lang.*

fun cellId(
    cell: KslExprInt3,
    gridCells: KslExprInt3,
) = cell.x + (cell.y * gridCells.x) + (cell.z * gridCells.x * gridCells.y)
