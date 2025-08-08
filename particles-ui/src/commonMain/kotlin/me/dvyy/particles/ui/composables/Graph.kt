package me.dvyy.particles.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.fabmax.kool.modules.compose.composables.layout.Box
import de.fabmax.kool.modules.compose.modifiers.Modifier
import de.fabmax.kool.modules.compose.modifiers.background
import de.fabmax.kool.modules.compose.modifiers.fillMaxWidth
import de.fabmax.kool.modules.compose.modifiers.height
import de.fabmax.kool.modules.ui2.dp
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.dvyy.particles.ui.nodes.GraphState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun Graph(
    graph: GraphState = remember { GraphState() },
    gatherData: suspend GraphState.() -> Unit,
    refreshRate: Duration = 0.1.seconds,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(graph) {
        withContext(Dispatchers.RenderLoop) {
            while (true) {
                gatherData(graph)
                delay(refreshRate)
            }
        }
    }
    Box(modifier.background(graph).height(400.dp).fillMaxWidth()) { }
}
