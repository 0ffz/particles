package me.dvyy.particles.helpers.kool

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.KoolContext
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.scene
import kotlinx.coroutines.*
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

object KoolTestExtension : BeforeAllCallback, AfterAllCallback {
    val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create("me", "dvyy", "particles")
    var ctx: KoolContext? = null
    var scene: Scene? = null
    override fun beforeAll(context: ExtensionContext) {
        val store = context.root.getStore(NAMESPACE)
        val koolContext = store.getOrComputeIfAbsent("kool-context") { createKoolContext() } as KoolContext
        ctx = koolContext
        // Add empty test scene
        scene = scene {  }.also { koolContext.addScene(it) }
    }

    override fun afterAll(context: ExtensionContext?) {
        // Clear all scenes
        ctx!!.removeScene(scene ?: return)
    }

    fun createKoolContext(): KoolContext = runBlocking {
        val context = CompletableDeferred<KoolContext>()
        CoroutineScope(Dispatchers.Default).launch {
            KoolApplication(KoolConfigJvm(renderBackend = KoolConfigJvm.Backend.VULKAN)) {
                context.complete(ctx)
            }
        }
        context.await()
    }
}
