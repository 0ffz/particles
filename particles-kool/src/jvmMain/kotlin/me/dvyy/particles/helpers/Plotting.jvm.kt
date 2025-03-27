package me.dvyy.particles.helpers

import org.jetbrains.letsPlot.awt.plot.PlotSvgExport
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.intern.toSpec

actual fun Plot.toSvg(): String {
    return PlotSvgExport.buildSvgImageFromRawSpecs(toSpec())
}