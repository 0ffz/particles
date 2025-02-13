package me.dvyy.particles

import com.charleskorn.kaml.*
import me.dvyy.particles.YamlHelpers.convertMapToNestedMap
import me.dvyy.particles.YamlHelpers.convertMapToYaml
import me.dvyy.particles.dsl.pairwise.UniformParameter
import org.openrndr.events.Event
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.Slider.ValueChangedEvent
import java.nio.file.Path
import kotlin.io.path.*

class YamlParameters(
    val path: Path,
    val uniforms: List<UniformParameter<*>>,
) {
    val yaml = Yaml.default
    val mutableUniforms: List<MutableUniform<String>> = uniforms.map {
        MutableUniform<String>(it as UniformParameter<String>, it.parameter.default)//readOrDefault as String)
    }
    val dirtyUniforms get() = mutableUniforms.filter { it.dirty == true }

    init {
        load(path)
    }

    //    fun propertyOrNull(key: String): String? = runCatching { property(key) }.getOrNull()
//    fun property(key: String): String = get(key).yamlScalar.content

    fun save(path: Path = this.path) {
        val pairs = mutableUniforms.map {
            val encoded = Yaml.default.encodeToString(it.uniform.parameter.serializer, it.value)
            it.uniform.parameter.path to encoded
        }.toMap() //.sortedBy { it.first }.joinToString()
        path.createParentDirectories().also { if (it.notExists()) it.createFile() }
        path.writeText(convertMapToYaml(convertMapToNestedMap(pairs)))
    }

    fun load(path: Path = this.path) {
        val node: YamlMap = yaml.parseToYamlNode(path.inputStream()).yamlMap

        fun get(key: String): YamlNode = key.split(".").fold(node) { acc: YamlNode, stringPart ->
            acc.yamlMap.get<YamlNode>(stringPart) ?: error("Key $key not found in config")
        }

        mutableUniforms.forEach {
            val readOrDefault = runCatching {
                Yaml.default.decodeFromYamlNode(
                    it.uniform.parameter.serializer,
                    get(it.uniform.parameter.path)
                )
            }.getOrNull() ?: it.uniform.parameter.default!!
            it.set(readOrDefault)
        }
    }

}

object YamlHelpers {
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

data class MutableUniform<T>(
    val uniform: UniformParameter<T>,
    var value: T,
    var dirty: Boolean = true,
) {
    val valueChanged = Event<T>("uniform-value-changed")
    fun get() = value

    fun set(value: T) {
        this.value = value
        dirty = true
        valueChanged.trigger(value)
    }
}
