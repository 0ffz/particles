package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuRow
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class VisualOptionsWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
//    private val configRepo: ConfigRepository,
) : FieldsWindow("Visual options", ui, Icons.eye) {
    val sizeList = listOf(Sizes.small, Sizes.medium, Sizes.large)

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
                    modifier.items(listOf("Small", "Medium", "Large"))
                        .selectedIndex(sizeList.indexOf(ui.uiSizes.use()))
                        .onItemSelected { ui.uiSizes.set(sizeList[it]) }
                }
            }
            MenuRow {
                Text("Recolor") {
                    labelStyle()
                    modifier.width(Grow.Std)
                }
//                Switch(viewModel.) {}
            }
        }
    }
}
