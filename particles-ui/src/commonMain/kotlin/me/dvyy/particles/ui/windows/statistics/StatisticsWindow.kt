import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.rendering.Text
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.Graph
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.Window
import me.dvyy.particles.ui.windows.statistics.StatisticsViewModel

@Composable
fun StatisticsWindow(
    viewModel: StatisticsViewModel = koinInject(),
    particles: ParticlesViewModel = koinInject(),
) = Window {
    val fps by viewModel.fps.collectAsState()

    Category("Statistics") {
        Text("Fps: $fps")
    }
    Category("Graphs") {
        Graph(particles.velocitiesHistogram, gatherData = {
            particles.updateVelocityHistogram()
        })
    }
}
