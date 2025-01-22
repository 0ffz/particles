import dsl.ParticlesConfiguration
import extensions.CustomCamera2D
import extensions.FPSDisplay
import org.openrndr.Fullscreen
import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DIM_GRAY
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.panel.style.defaultStyles

data class FieldsApplication(
    val config: ParticlesConfiguration,
) {
    fun start() = application {
        configure {
//        width = 1000
//        height = 600
            fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
            windowResizable = true
            vsync = false
        }

        program {
            window.presentationMode = PresentationMode.MANUAL
            // Create simulation settings and attach to the gui
            val gui = GUI(
                appearance = GUIAppearance(
                    baseColor = ColorRGBa.DIM_GRAY,
                ),
                defaultStyles = defaultStyles(
                    controlFontSize = 17.0,
                )
            ).apply {
                compartmentsCollapsedByDefault = false

                add(SimulationConstants)
                add(SimulationSettings)
            }
            extend(CustomCamera2D(gui = gui))
            extend(gui) // Load saved values right away
            extend(FPSDisplay(gui) { SimulationSettings.step })
            extend(FieldsGPU(drawer.bounds, config))
        }
    }

}
