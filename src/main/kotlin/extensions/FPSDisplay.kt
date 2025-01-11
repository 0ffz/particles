package extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44

class FPSDisplay(
    val getCurrentStep: () -> Int,
) : Extension {
    override var enabled: Boolean = true
    
    var frames = 0
    var startTime: Long = 0L
    
    override fun setup(program: Program) {
        startTime = System.nanoTime()
    }
    
    override fun afterDraw(drawer: Drawer, program: Program) {
        frames++
        
        drawer.isolated {
            // -- set view and projections
            drawer.view = Matrix44.IDENTITY
            drawer.ortho()

            drawer.fill = ColorRGBa.PINK
            val now = System.nanoTime()
            drawer.text("fps: ${(frames / ((now - startTime) / 1e9)).toInt()}", y = 20.0, x = 2.0)
            drawer.text("simulation rate: ${(getCurrentStep() / ((now - startTime) / 1e9)).toInt()}", y = 40.0, x = 2.0)
        }
    }
}
