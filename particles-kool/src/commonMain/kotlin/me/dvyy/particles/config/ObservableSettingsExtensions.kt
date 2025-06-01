package me.dvyy.particles.config

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

inline fun <T> ObservableSettings.getFlow(
    key: String,
    defaultValue: T,
    scope: CoroutineScope,
    crossinline getter: (String, T) -> T,
    crossinline setter: (String, T) -> Unit,
    crossinline addListener: ObservableSettings.(String, T, (T) -> Unit) -> SettingsListener,
): MutableStateFlow<T> {
    val flow = MutableStateFlow(
        getter(
            key,
            defaultValue
        )
    )
    val listener = addListener(key, defaultValue) { newValue ->
        flow.update { newValue }
    }
    scope.launch {
        flow.collect { setter(key, it) }
    }.invokeOnCompletion {
        listener.deactivate()
    }
    return flow
}

inline fun <reified T> ObservableSettings.getFlow(
    key: String,
    defaultValue: T,
    scope: CoroutineScope,
    serializer: KSerializer<T> = serializer(),
) = getFlow<T>(
    key,
    defaultValue,
    scope,
    getter = { key, defaultValue ->
        YamlHelpers.yaml.decodeFromString(serializer, getStringOrNull(key) ?: return@getFlow defaultValue)
    },
    setter = { key, newValue -> putString(key, YamlHelpers.yaml.encodeToString(serializer, newValue)) },
    addListener = { key, defaultValue, callback ->
        addStringOrNullListener(key) {
            callback(
                if (it == null) defaultValue else YamlHelpers.yaml.decodeFromString(serializer, it)
            )
        }
    }
)
