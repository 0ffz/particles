package me.dvyy.particles.config

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

@OptIn(ExperimentalSettingsApi::class)
actual fun createSettings(): ObservableSettings {
    return PreferencesSettings(Preferences.userRoot().node("me.dvyy.particles"))
}
