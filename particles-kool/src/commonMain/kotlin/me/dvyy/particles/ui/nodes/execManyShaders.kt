package me.dvyy.particles.ui.nodes

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.pipeline.ComputePass
import de.fabmax.kool.pipeline.ComputeShader
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.launchOnMainThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

inline fun <T> execManyShaders(
    scene: Scene,
    setup: (ComputePass) -> Unit,
    crossinline read: suspend () -> T,
): Deferred<T> {
    val computePass = ComputePass("single-shot")
    setup(computePass)
    scene.addComputePass(computePass)

    val deferred = CompletableDeferred<T>()

    computePass.onAfterPass {
        computePass.isEnabled = false
        launchOnMainThread {
            try {
                deferred.complete(read())
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            } finally {
                scene.removeComputePass(computePass)
            }
        }
    }

    return deferred

}

inline fun <T> execShader(
    scene: Scene,
    shader: ComputeShader,
    numGroups: Vec3i = Vec3i.ONES,
    crossinline read: suspend () -> T,
): Deferred<T> {
    return execManyShaders(scene, setup = {
        it.addTask(shader, numGroups)
    }, read)
}
