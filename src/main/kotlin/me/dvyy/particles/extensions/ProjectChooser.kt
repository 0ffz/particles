package me.dvyy.particles.extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.style.*
import org.openrndr.resourceUrl
import java.nio.file.Path

class ProjectChooser(
    val onProjectSelected: (Path) -> Unit,
) : Extension {
    override var enabled: Boolean = true

    override fun setup(program: Program) {
        program.apply {
            extend(controlManager {
                controlManager.fontManager.register("JetBrains Mono", resourceUrl("/data/fonts/default.otf"))
                styleSheet(has class_ "side-bar") {
                    height = 100.percent
                    width = 200.px
                    display = Display.FLEX
                    flexDirection = FlexDirection.Column
                    paddingLeft = 10.px
                    paddingRight = 10.px
                    background = Color.RGBa(ColorRGBa.GRAY)
                    fontFamily = "JetBrains Mono"
                }
                styleSheet(has type "slider") {
                    marginTop = 25.px
                    marginBottom = 25.px
                }
                layout {
                    div("side-bar") {
                        style?.width = 100.percent
                        button("Open Project") {
                            events.clicked.listen {
                                openFileDialog {
                                    onProjectSelected(it.toPath())
                                }
                            }
                        }

                        //                    config.uniforms.forEach { uniform ->
                        //                        slider {
                        //                            label = uniform.configKey
                        //                            value = uniform.value
                        //                            events.valueChanged.listen {
                        //                                uniform.update(it.newValue)
                        //                            }
                        //                        }
                        //                    }
                    }
                }
            })
        }
    }
}
