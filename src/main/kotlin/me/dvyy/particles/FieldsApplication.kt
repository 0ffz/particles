package me.dvyy.particles

import me.dvyy.particles.dsl.ParticlesConfiguration
import me.dvyy.particles.dsl.ParticlesDSL
import me.dvyy.particles.extensions.CustomCamera2D
import me.dvyy.particles.extensions.FPSDisplay
import me.dvyy.particles.scripting.ParticlesScripting
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.drawThread
import org.openrndr.draw.launch
import org.openrndr.extra.color.presets.DIM_GRAY
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.panel.style.defaultStyles
import kotlin.io.path.Path

class FieldsApplication {
    val scripting = ParticlesScripting()
    val managerExtension = object: Extension {
        override var enabled: Boolean = true

        override fun beforeDraw(drawer: Drawer, program: Program) {

        }
    }

    var program: Program? = null

    fun openProject(config: ParticlesConfiguration) = program!!.apply {
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
        extend(FieldsGPU(drawer.bounds, config, onResetRequested = {
            program.launch {
                closeProject()
                loadProjectFromPath()
            }
        }))
    }

    fun closeProject() = program!!.apply {
        for (extension in program.extensions) {
            extension.shutdown(program)
        }
        program.extensions.clear()
    }

    fun loadProjectFromPath() {
        val defaultPath = Path("particles.main.kts")
        val dsl = scripting.evalResult<ParticlesDSL>(defaultPath.toFile()) ?: return
        openProject(dsl.build())
    }

    fun start() = application {
        configure {
//            fullscreen = if (config.application.fullscreen) Fullscreen.CURRENT_DISPLAY_MODE else Fullscreen.DISABLED
            windowResizable = true
//            width = config.application.width
//            height = config.application.height
            vsync = false
        }

        this@FieldsApplication.program = program {
            window.presentationMode = PresentationMode.MANUAL

//            this.dispatcher.launch {
                loadProjectFromPath()
//            }
            keyboard.keyDown.listen {
                if (it.name == "s") {
                    program.launch {
                        closeProject()
                        loadProjectFromPath()
                    }
                }
            }
        }
        SimulationConstants.restartEvent.listen {
            println("Restarting simulation...")
            // Await simulation thread stop
            program.launch {
                closeProject()
                loadProjectFromPath()
            }
        }
        SimulationConstants.resetPositionsEvent.listen {
            program.extensions.filterIsInstance<FieldsGPU>().singleOrNull()?.initializeRandomPositions()
        }
    }
}
