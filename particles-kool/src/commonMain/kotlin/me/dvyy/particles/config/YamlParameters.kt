package me.dvyy.particles.config

import com.charleskorn.kaml.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import me.dvyy.particles.config.YamlHelpers.convertMapToNestedMap
import me.dvyy.particles.config.YamlHelpers.convertMapToYaml
import me.dvyy.particles.helpers.FileSystemUtils

class YamlParameters(
    val path: String,
    val scope: CoroutineScope,
) {
    private val _overrides = mutableMapOf<String, YamlNode>()
    val overrides = MutableSharedFlow<Map<String, YamlNode>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun update(key: String, value: YamlNode) {
        _overrides[key] = value
        overrides.tryEmit(_overrides)
    }

    fun save(path: String = this.path) {
        val encoded = YamlHelpers.yaml.encodeToString(_overrides)
        FileSystemUtils.write(path, encoded)
    }

    fun load(path: String = this.path) {
        val decoded = runCatching {
            YamlHelpers.yaml.decodeFromString<Map<String, YamlNode>>(FileSystemUtils.read(path) ?: "{}")
        }.getOrDefault(emptyMap())

        _overrides.clear()
        _overrides.putAll(decoded)
        overrides.tryEmit(_overrides)
    }

    fun reset() {
        _overrides.clear()
        overrides.tryEmit(_overrides)
    }
}

object YamlHelpers {
    fun YamlNode.getPath(key: String): YamlNode = key.split(".").fold(this) { acc: YamlNode, stringPart ->
        acc.yamlMap.get<YamlNode>(stringPart) ?: error("Key $key not found in config")
    }

    inline fun <reified T> YamlNode?.decode(default: T): T {
        if (this == null) return default
        return runCatching { yaml.decodeFromYamlNode<T>(serializer<T>(), this) }.getOrDefault(default)
    }

    fun <T> YamlNode.decode(serializer: KSerializer<T>): T {
        return yaml.decodeFromYamlNode<T>(serializer, this)
    }

    inline fun <reified T> YamlMap.get(yamlPath: String, default: T): T {
        return runCatching { YamlHelpers.yaml.decodeFromYamlNode<T>(serializer<T>(), getPath(yamlPath)) }
            .getOrDefault(default)
    }

//    inline fun <reified T> YamlMap.with(yamlPath: String, value: T): YamlMap {
//        this.copy
//    }

    val yaml = Yaml(
        configuration = YamlConfiguration(
            singleLineStringStyle = SingleLineStringStyle.Plain,
            encodeDefaults = false,
        )
    )

    // Map to yaml conversion helpers
    fun convertMapToNestedMap(flatMap: Map<String, Any>): Map<String, Any> {
        val nestedMap = mutableMapOf<String, Any>()
        for ((key, value) in flatMap) {
            val parts = key.split(".")
            var currentLevel = nestedMap
            // Traverse (or create) nested maps for each part except the last
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                if (currentLevel[part] !is MutableMap<*, *>) {
                    currentLevel[part] = mutableMapOf<String, Any>()
                }
                @Suppress("UNCHECKED_CAST")
                currentLevel = currentLevel[part] as MutableMap<String, Any>
            }
            // Set the final value
            currentLevel[parts.last()] = value
        }
        return nestedMap
    }

    // Helper function to convert a nested map to a YAML string using recursion.
    fun nestedMapToYaml(nestedMap: Map<String, Any>, indent: Int = 0): String {
        val sb = StringBuilder()
        val indentStr = "  ".repeat(indent)
        for ((key, value) in nestedMap) {
            when (value) {
                is Map<*, *> -> {
                    sb.append("$indentStr$key:\n")
                    @Suppress("UNCHECKED_CAST")
                    sb.append(nestedMapToYaml(value as Map<String, Any>, indent + 1))
                }

                else -> {
                    sb.append("$indentStr$key: $value\n")
                }
            }
        }
        return sb.toString()
    }

    // Function that converts a flat map directly to a YAML string.
    fun convertMapToYaml(flatMap: Map<String, Any>): String {
        val nestedMap = convertMapToNestedMap(flatMap)
        return nestedMapToYaml(nestedMap)
    }
}

