package me.dvyy.particles

import com.charleskorn.kaml.*
import me.dvyy.particles.dsl.pairwise.UniformParameter
import java.nio.file.Path
import kotlin.io.path.inputStream

class YamlParameters(
    path: Path,
    uniforms: List<UniformParameter>,
) {
    val yaml = Yaml.Companion.default
    val node: YamlMap = yaml.parseToYamlNode(path.inputStream()).yamlMap

    private fun get(key: String): YamlNode = key.split(".").fold(node) { acc: YamlNode, stringPart ->
        acc.yamlMap.get<YamlNode>(stringPart) ?: error("Key $key not found in config")
    }

    fun propertyOrNull(key: String): String? = runCatching { property(key) }.getOrNull()
    fun property(key: String): String = get(key).yamlScalar.content

    val mutableUniforms: List<MutableUniform> = uniforms.map {
        val readOrDefault =
            runCatching { Yaml.default.decodeFromYamlNode(it.parameter.serializer, get(it.parameter.path)) }
                .getOrNull()
                ?: it.parameter.default!!
        MutableUniform(it, readOrDefault)
    }
}

data class MutableUniform(
    val uniform: UniformParameter,
    var value: Any,
    var dirty: Boolean = false,
) {
    fun get() = value

    fun set(value: Any) {
        this.value = value
        dirty = true
    }
}
