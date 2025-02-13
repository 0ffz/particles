package me.dvyy.particles

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.extensions.CustomCamera2D
import me.dvyy.particles.extensions.FPSDisplay
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DIM_GRAY
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.launch
import org.openrndr.panel.style.defaultStyles
import kotlin.io.path.Path
import kotlin.io.path.inputStream

class FieldsApplication {
    var program: Program? = null

    fun openProject(config: ParticlesConfig) = program!!.apply {
        val parameters = YamlParameters(Path("parameters.yml"), config.configurableUniforms)
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
        val defaultPath = Path("particles.yml")
        val config = Yaml.default.decodeFromStream<ParticlesConfig>(defaultPath.inputStream())
        openProject(config)
    }

    fun start() = application {
        configure {
//            fullscreen = if (config.application.fullscreen) Fullscreen.CURRENT_DISPLAY_MODE else Fullscreen.DISABLED
            windowResizable = true
            width = 1280
            height = 720
            vsync = false
        }

        this@FieldsApplication.program = program {
            window.presentationMode = PresentationMode.MANUAL
//            extend(ProjectChooser {
//                loadProjectFromPath()
//            })
            loadProjectFromPath()

//            keyboard.keyDown.listen {
//                if (it.name == "s") {
//                    program.launch {
//                        closeProject()
//                        loadProjectFromPath()
//                    }
//                }
//            }
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
