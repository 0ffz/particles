import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.toString
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.composables.Graph
import me.dvyy.particles.ui.composables.Subcategory
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.ParticlesViewModel
import me.dvyy.particles.ui.windows.statistics.StatisticsViewModel

@Composable
fun StatisticsWindow(
    viewModel: StatisticsViewModel = koinInject(),
    particles: ParticlesViewModel = koinInject(),
    configRepo: ConfigRepository = koinInject(),
) {
    Category("Stats") {
        val fps by viewModel.fps.collectAsState()
        val simsPs = configRepo.passesPerFrame.value * fps
        Text("Simulation speed: ${simsPs.toString(2)} sims/s")
    }
    Category("Graphs") {
        Subcategory("Velocity Histogram") {
            Graph(particles.velocitiesHistogram, gatherData = {
                particles.updateVelocityHistogram()
            })
        }
    }
}
