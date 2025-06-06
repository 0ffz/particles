package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.render.ParticleColor
import me.dvyy.particles.render.UiScale
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuRow
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class VisualOptionsWindow(
    ui: AppUI,
    private val settings: AppSettings,
    private val scope: CoroutineScope,
) : FieldsWindow("Visual options", ui, Icons.eye) {
    //    val sizeList = listOf(Sizes.small, Sizes.medium, Sizes.large)
    val coloring = settings.ui.coloring.asMutableState(scope)
    val size = settings.ui.scale.asMutableState(scope)
    val shouldCalibrateFPS = settings.ui.shouldCalibrateFPS.asMutableState(scope)
    val targetFPS = settings.ui.targetFPS.asMutableState(scope)

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        withVerticalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std, Grow.Std) {
            Category(
                "FPS Calibration",
                desc = "Adjust the number of simulation steps per frame to reach a target fps. This may decrease simulation speed in favor of a smooth view."
            ) {
                MenuRow {
                    Text("Enabled") {
                        labelStyle()
                        modifier.width(Grow.Std)
                    }
                    Checkbox(shouldCalibrateFPS.use()) {
                        modifier.onToggle {
                            settings.ui.shouldCalibrateFPS.update { !it }
                        }
                    }
                }
                MenuRow {
                    Text("Target fps") {
                        labelStyle()
                        modifier.width(Grow.Std)
                    }
                    TextField(targetFPS.use().toString()) {
                        modifier.onChange {
                            val int = it.toIntOrNull()
                            if (int != null) settings.ui.targetFPS.update { int }
                        }
                    }
                }
            }
            Category("UI") {
                MenuRow {
                    Text("UI size") {
                        labelStyle()
                        modifier.width(Grow.Std)
                    }
                    ComboBox {
                        modifier.items(UiScale.entries.map { it.name.lowercase().capitalize() })
                            .selectedIndex(size.use().ordinal)
                            .onItemSelected { new -> settings.ui.scale.update { UiScale.entries[new] } }
                    }
                }
                MenuRow {
                    Text("Particle color") {
                        labelStyle()
                        modifier.width(Grow.Std)
                    }
                    ComboBox {
                        modifier.items(ParticleColor.entries.map { it.name.lowercase().capitalize() })
                            .selectedIndex(coloring.use().ordinal)
                            .onItemSelected { color -> settings.ui.coloring.update { ParticleColor.entries[color] } }
                    }
                }
            }
        }
    }
}
