package me.dvyy.particles.ui.nodes

import de.fabmax.kool.util.Color

sealed interface GraphStyle {
    data class Line(val color: Color) : GraphStyle

    data class Bar(val width: Double) : GraphStyle
}
