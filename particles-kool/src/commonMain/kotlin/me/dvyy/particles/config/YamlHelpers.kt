package me.dvyy.particles.config

import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.yamlMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.collections.iterator

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
        return runCatching { yaml.decodeFromYamlNode<T>(serializer<T>(), getPath(yamlPath)) }
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
