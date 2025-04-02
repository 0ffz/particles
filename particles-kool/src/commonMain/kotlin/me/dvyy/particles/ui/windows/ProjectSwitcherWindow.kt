package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.components.IconButton
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.TRANSPARENT
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class ProjectSwitcherWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
    private val settings: AppSettings,
    private val scope: CoroutineScope,
) : FieldsWindow(
    name = "Projects",
    ui = ui,
    icon = Icons.folderOpen,
    preferredWidth = 600f,
) {
    val recentPaths = settings.recentProjectPaths.asMutableState(scope)

    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        withVerticalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std, Grow.Std) {
            Button("Open project") {
                modifier.onClick {
                    viewModel.attemptOpenProject()
                }.margin(sizes.smallGap)
            }

            Column(Grow.Std) {
                recentPaths.use().forEach { path ->
                    Box(Grow.Std) {
                        modifier
                            .margin(sizes.smallGap)
                            .padding(4.dp)
                            .border(RoundRectBorder(colors.onBackgroundAlpha(0.5f), 10.dp, 2.dp))

//                        Image {
//                            modifier.size(Grow.Std, 200.dp)
//                        }
                        Row(Grow.Std) {
//                            modifier.backgroundColor(colors.backgroundAlpha(0.5f)).alignY(AlignmentY.Bottom)
                            Button(Path(path).name.substringBeforeLast(".")) {
                                modifier.alignY(AlignmentY.Center)
                                    .colors(
                                        buttonColor = Color.TRANSPARENT,
                                        buttonHoverColor = Color.WHITE.withAlpha(0.1f)
                                    )
                                    .width(Grow.Std)
                                    .font(sizes.normalText)
                                    .onClick {
                                        when {
                                            it.isLeftClick -> viewModel.openProject(path)
                                            it.pointer.isMiddleButtonClicked -> viewModel.removeProject(path)
                                        }
                                    }
                                    .textAlignX(AlignmentX.Start)
                            }
                            IconButton(Icons.x, onClick = {
                                viewModel.removeProject(path)
                            })
                        }
                    }
                }
            }
        }
    }
}

