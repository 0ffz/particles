package me.dvyy.particles

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.extensions.CustomCamera2D
import me.dvyy.particles.extensions.FPSDisplay
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Display
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

class FieldsApplication {
    var program: Program? = null

    fun openProject(config: ParticlesConfig) = program!!.apply {
        val parameters = YamlParameters(
            Path("parameters.yml"),
            SimulationSettings().uniforms + config.configurableUniforms
        )
        // Create simulation settings and attach to the gui
//        val gui = GUI(
//            appearance = GUIAppearance(
//                baseColor = ColorRGBa.DIM_GRAY,
//            ),
//            defaultStyles = defaultStyles(
//                controlFontSize = 17.0,
//            )
//        ).apply {
//            compartmentsCollapsedByDefault = false
//
//            add(SimulationConstants)
////            add(SimulationSettings)
//        }
//        extend(gui) // Load saved values right away
        extend(controlManager {
            controlManager.fontManager.register("JetBrains Mono", resourceUrl("/data/fonts/default.otf"))
            styleSheet(has class_ "side-bar") {
                height = 100.percent
                width = 200.px
                display = Display.FLEX
                flexDirection = FlexDirection.Column
                paddingLeft = 5.px
                paddingRight = 5.px
                background = Color.RGBa(ColorRGBa.GRAY)
                fontFamily = "JetBrains Mono"
                fontSize = 15.px
            }
            styleSheet(has type "slider") {
                fontSize = 15.px
//                marginTop = 25.px
//                marginBottom = 25.px
            }
            layout {
                div("side-bar") {
                    parameters.mutableUniforms
                        .groupBy { it.uniform.parameter.path.substringBeforeLast(".").substringAfter(".") }
                        .forEach { (commonKey, uniforms) ->
                            text(commonKey)
                            uniforms.forEach { uniform ->
                                slider {
                                    label = uniform.uniform.name
                                    precision = uniform.uniform.precision
                                    range = Range(uniform.uniform.range.start, uniform.uniform.range.endInclusive)
                                    value = (uniform.value as String).toDouble()
                                    uniform.valueChanged.listen {
                                        value = it.toDouble()
                                    }
                                    events.valueChanged.listen {
                                        uniform.set(it.newValue.toString())
                                    }
                                }
                            }
                        }
                    button(label = "Save") {
                        clicked {
                            parameters.save()
                        }
                    }
                    button(label = "Load") {
                        clicked {
                            parameters.load()
                        }
                    }
                    button(label = "Reset positions") {
                        clicked {
                            SimulationConstants.resetPositionsEvent.trigger(Unit)
                        }
                    }
                    button(label = "Reload program") {
                        clicked {
                            SimulationConstants.restartEvent.trigger(Unit)
                        }
                    }
                }
            }
        })
        val step = AtomicInteger(0)
        extend(CustomCamera2D())
        extend(FPSDisplay() { step.get() })
        extend(FieldsGPU(drawer.bounds, config, parameters, onResetRequested = {
            program.launch {
                closeProject()
                loadProjectFromPath()
            }
        }, step))
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
