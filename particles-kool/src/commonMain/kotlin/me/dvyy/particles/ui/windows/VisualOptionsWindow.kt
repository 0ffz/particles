package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.render.ParticleColor
import me.dvyy.particles.render.UiScale
import me.dvyy.particles.ui.AppSizes
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuRow
import me.dvyy.particles.ui.helpers.UiSizes
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class VisualOptionsWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
    private val settings: AppSettings,
    private val scope: CoroutineScope,
) : FieldsWindow("Visual options", ui, Icons.eye) {
//    val sizeList = listOf(Sizes.small, Sizes.medium, Sizes.large)
    val coloring = settings.ui.coloring.asMutableState(scope)
    val size = settings.ui.scale.asMutableState(scope)
    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        withVerticalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std, Grow.Std) {
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
                        .onItemSelected { color -> settings.ui.coloring.update { ParticleColor.entries[color]  } }
                }
            }
        }
    }
}
