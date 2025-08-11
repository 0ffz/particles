package me.dvyy.particles.ui.windows.visual_options

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.update
import me.dvyy.particles.config.AppSettings
import me.dvyy.particles.render.ParticleColor
import me.dvyy.particles.render.UiScale
import me.dvyy.particles.ui.composables.Category
import me.dvyy.particles.ui.helpers.koinInject
import me.dvyy.particles.ui.windows.live_parameters.MenuCheckbox
import me.dvyy.particles.ui.windows.live_parameters.MenuEnum
import me.dvyy.particles.ui.windows.live_parameters.MenuNumber

@Composable
fun VisualOptionsWindow(
    settings: AppSettings = koinInject(),
) {
    Category(
        "FPS Calibration",
        desc = "Adjust the number of simulation steps per frame to reach a target fps. This may decrease simulation speed in favor of a smooth view."
    ) {
        val shouldCalibrate by settings.ui.shouldCalibrateFPS.collectAsState()
        val targetFps by settings.ui.targetFPS.collectAsState()
        MenuNumber(
            "Target FPS",
            targetFps,
            onValueChange = { new -> settings.ui.targetFPS.update { new.toInt() } }
        )
        MenuCheckbox(
            "Enabled",
            shouldCalibrate,
            onValueChange = { new -> settings.ui.shouldCalibrateFPS.update { new } }
        )
    }
    Category("UI") {
        val scale by settings.ui.scale.collectAsState()
        val particleColoring by settings.ui.coloring.collectAsState()
        MenuEnum<UiScale>("Scale", scale, onValueChange = { new -> settings.ui.scale.update { new } })
        MenuEnum<ParticleColor>("Particle color", particleColoring, onValueChange = { new -> settings.ui.coloring.update { new } })
    }
}
