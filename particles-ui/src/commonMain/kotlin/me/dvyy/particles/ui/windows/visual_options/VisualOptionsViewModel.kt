package me.dvyy.particles.ui.windows.visual_options

import kotlinx.coroutines.CoroutineScope
import me.dvyy.particles.config.AppSettings

class VisualOptionsViewModel(
    val scope: CoroutineScope,
    val settings: AppSettings,
) {
    val scale = settings.ui.scale
}
