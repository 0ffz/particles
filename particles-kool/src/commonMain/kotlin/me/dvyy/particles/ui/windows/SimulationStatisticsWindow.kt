package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import me.dvyy.particles.clustering.ParticleClustering
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.helpers.MenuRow
import me.dvyy.particles.ui.helpers.MenuSlider2
import me.dvyy.particles.ui.helpers.labelStyle
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel

class SimulationStatisticsWindow(
    ui: AppUI,
    private val viewModel: ParticlesViewModel,
    private val configRepo: ConfigRepository,
    private val settings: AppSettings,
    private val clustering: ParticleClustering,
    private val scope: CoroutineScope,
) : FieldsWindow(
    name = "Simulation Stats",
    ui = ui,
    icon = Icons.chartSpline,
    preferredWidth = 600f,
) {
    val simsPs = mutableStateOf(0.0)
    val fps = mutableStateOf(0.0)
    val clusterOptions = settings.clusterOptions.asMutableState(scope)


    override fun UiScope.windowContent() = ScrollArea(
        withHorizontalScrollbar = false,
        withVerticalScrollbar = false,
        containerModifier = { it.background(null) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std, Grow.Std) {

            Category("Graphs") {
                Subcategory("Velocity Histogram") {
                    var counter by remember(0)
                    // Update velocity histogram every 25 frames
                    surface.onEachFrame {
                        counter++
                        if (counter % 10 == 0) launchOnMainThread {
                            viewModel.updateVelocityHistogram()
                        }
                    }
                    ParameterGraph(viewModel.velocitiesHistogram)
                }
                Subcategory("Cluster size distribution") {
                    val opts by clusterOptions
                    MenuRow {
                        Text("Enabled") { labelStyle(); modifier.width(Grow.Std) }
                        Switch(clusterOptions.use().enabled) {
                            modifier.onToggle {
                                settings.clusterOptions.update {
                                    it.copy(enabled = !it.enabled)
                                }
                            }
                        }
                    }
                    MenuSlider2(
                        "Radius",
                        opts.radius.toFloat(),
                        min = 0f,
                        max = 30f,
                        onChange = { new ->
                            settings.clusterOptions.update {
                                it.copy(radius = new.toDouble())
                            }
                        },
                    )
                    MenuSlider2(
                        "Min Points",
                        opts.minPoints.toFloat(),
                        min = 0f,
                        max = 30f,
                        precision = 0,
                        onChange = { new ->
                            settings.clusterOptions.update {
                                it.copy(minPoints = new.toInt())
                            }
                        },
                    )
                    Image(viewModel.plotTexture) {
                        modifier.width(Grow.Std)
                    }
                    MenuRow {
                        Button("Save") {
                            modifier.onClick {
                                viewModel.saveClusterData()
                            }.margin(end = sizes.smallGap)
                        }
                        Button("Calculate") {
                            val clusterOptions = settings.clusterOptions.value
                            modifier.onClick {
                                clustering.calculateClusters(clusterOptions)
                            }
                        }
                    }
                }
            }
        }
    }
}
