@file:OptIn(ExperimentalSettingsApi::class)

package me.dvyy.particles.config

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.dvyy.particles.render.ParticleColor
import me.dvyy.particles.render.UiScale

class AppSettings() {
    private val scope = CoroutineScope(Dispatchers.RenderLoop)
    val settings: ObservableSettings = createSettings()
    val ui = UiSettings(settings, scope)
    val recentProjectPaths = settings.getFlow("recentProjectPaths", listOf<String>(), scope)
    val clusterOptions = settings.getFlow("clusterOptions", ClusterOptions(enabled = false), scope)

    fun removeKeys(startingWith: String) {
        settings.keys.filter { it.startsWith(startingWith) }.forEach {
            settings.remove(it)
        }
    }
}

class UiSettings(
    settings: ObservableSettings,
    scope: CoroutineScope,
) {
    val coloring = settings.getFlow("coloring", ParticleColor.TYPE, scope)
    val scale = settings.getFlow("scale", UiScale.LARGE, scope)
}


expect fun createSettings(): ObservableSettings
