package me.dvyy.particles.helpers

import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun Scene.launch(scope: CoroutineScope = CoroutineScope(Dispatchers.RenderLoop), run: suspend CoroutineScope.() -> Unit) {
    onRelease {
        scope.cancel()
    }
    scope.launch { run() }
}
