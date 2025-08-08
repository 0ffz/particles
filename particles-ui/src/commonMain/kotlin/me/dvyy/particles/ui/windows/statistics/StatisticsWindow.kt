import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.toString
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.Graph
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import me.dvyy.particles.ui.windows.statistics.StatisticsViewModel

@Composable
fun StatisticsWindow(
    viewModel: StatisticsViewModel = koinInject(),
    particles: ParticlesViewModel = koinInject(),
) {
    Category("Stats") {
        val fps by viewModel.fps.collectAsState()
        val simsPs = particles.passesPerFrame.value * fps
        Text("Simulation speed: ${simsPs.toString(2)} sims/s")
    }
    Category("Graphs") {
        Graph(particles.velocitiesHistogram, gatherData = {
            particles.updateVelocityHistogram()
        })
    }
}
