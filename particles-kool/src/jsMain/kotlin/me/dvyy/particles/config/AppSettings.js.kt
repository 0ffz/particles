package me.dvyy.particles.config

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.StorageSettings
import com.russhwolf.settings.observable.makeObservable

actual fun createSettings(): ObservableSettings {
    return StorageSettings().makeObservable()
}
