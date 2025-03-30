package me.dvyy.particles.config

import com.charleskorn.kaml.YamlNode
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class ParameterOverrides(
    val settings: AppSettings,
) {
    private val scope = CoroutineScope(Dispatchers.RenderLoop)
    private val _overrides = mutableMapOf<String, YamlNode>()
    val overrides = MutableSharedFlow<Map<String, YamlNode>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            load()
        }
    }

    fun update(key: String, value: YamlNode) {
        _overrides[key] = value
        settings.settings.putString("$PREFIX$key", YamlHelpers.yaml.encodeToString(value))
        overrides.tryEmit(_overrides)
    }

    fun load() {
        settings.settings.keys.filter { it.startsWith(PREFIX) }.forEach {
            val settingString = settings.settings.getString(it, "{}")
            _overrides[it.removePrefix(PREFIX)] = YamlHelpers.yaml.parseToYamlNode(settingString)
        }
        overrides.tryEmit(_overrides)
    }

    fun reset() {
        settings.removeKeys(startingWith = PREFIX)
        _overrides.clear()
        overrides.tryEmit(_overrides)
    }

    companion object {
        const val PREFIX = "param-overrides/"
    }
}

