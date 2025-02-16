package me.dvyy.particles

import com.charleskorn.kaml.*
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.particles.YamlHelpers.convertMapToNestedMap
import me.dvyy.particles.YamlHelpers.convertMapToYaml

object FileSystemUtils {
    fun read(path: Path): String = SystemFileSystem.source(path).buffered().use {
        it.readString()
    }

    fun write(path: Path, content: String) {
        SystemFileSystem.sink(path).buffered().use {
            it.writeString(content)
        }
    }
}

class YamlParameters(
    val path: Path,
    val scope: CoroutineScope,
//    val uniforms: List<UniformParameter<*>>,
) {
    val yaml = Yaml.default

    //    val content = mutableStateOf<YamlMap?>(null)
    val content = MutableSharedFlow<YamlMap>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    data class StateWithSerializer<T>(val state: MutableStateValue<T>, val serializer: KSerializer<T>) {
        fun encodeToString() = Yaml.default.encodeToString<T>(serializer, state.value)
    }

    val mutableStates = mutableMapOf<String, StateWithSerializer<*>>()
    private val params = mutableListOf<MutableStateValue<*>>()
//    val mutableUniforms: List<MutableUniform<String>> = uniforms.map {
//        MutableUniform<String>(it as UniformParameter<String>, it.parameter.default)//readOrDefault as String)
//    }
//    val dirtyUniforms get() = mutableUniforms.filter { it.dirty == true }

    init {
        load(path)
    }

//    fun getNode(yamlPath: String): YamlNode =
//        content.getPath(yamlPath)
//
//    fun <T> get(yamlPath: String, serializer: DeserializationStrategy<T>): Flow<T> =
//        getNode(yamlPath)
//            .map { yaml.decodeFromYamlNode(serializer, it) }
//            .catch { logW { "Error reading config at $yamlPath: ${it.message}" } }
//
//    inline fun <reified T> get(yamlPath: String): Flow<T> = get(yamlPath, serializer<T>())

    inline fun <reified T> get(
        yamlPath: String,
        default: T,
        serializer: KSerializer<T> = serializer<T>(),
    ): MutableStateValue<T> {
        val state = mutableStates.getOrPut(yamlPath) {
            StateWithSerializer(mutableStateOf<T>(default), serializer)
        } as StateWithSerializer<T>
        require(serializer == state.serializer) { "Tried reading $yamlPath with serializer $serializer, but was already registered with ${state.serializer}" }

        scope.launch {
            content.map { it.getPath(yamlPath) }
                .map { yaml.decodeFromYamlNode(serializer, it) }
                .catch { logW { "Error reading config at $yamlPath: ${it.message}" } }
                .collect { state.state.set(it) }
        }
        return state.state
    }
    //    fun propertyOrNull(key: String): String? = runCatching { property(key) }.getOrNull()
//    fun property(key: String): String = get(key).yamlScalar.content

    fun save(path: Path = this.path) {
        println(mutableStates)
        val pairs = mutableStates.map { (key, state) ->
            val encoded = state.encodeToString()
            key to encoded
        }.toMap()
        val output = convertMapToYaml(convertMapToNestedMap(pairs))
//        val pairs = mutableUniforms.map {
//            val encoded = Yaml.default.encodeToString(it.uniform.parameter.serializer, it.value)
//            it.uniform.parameter.path to encoded
//        }.toMap() //.sortedBy { it.first }.joinToString()

        FileSystemUtils.write(Path(path.parent!!, path.name + "test"), output)
//        path.createParentDirectories().also { if (it.notExists()) it.createFile() }
    }

    fun load(path: Path = this.path) {
        val node: YamlMap = yaml.parseToYamlNode(FileSystemUtils.read(path)).yamlMap

        content.tryEmit(node)
//        params
//            val readOrDefault = runCatching {
//                Yaml.default.decodeFromYamlNode(
//                    it.uniform.parameter.serializer,
//                    get(it.uniform.parameter.path)
//                )
//            }.getOrNull() ?: it.uniform.parameter.default!!
//            it.set(readOrDefault)
    }

    companion object {
        fun YamlNode.getPath(key: String): YamlNode = key.split(".").fold(this) { acc: YamlNode, stringPart ->
            acc.yamlMap.get<YamlNode>(stringPart) ?: error("Key $key not found in config")
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

//data class MutableUniform<T>(
//    val uniform: UniformParameter<T>,
//    var value: T,
//    var dirty: Boolean = true,
//) {
//    val valueChanged = Event<T>("uniform-value-changed")
//    fun get() = value
//
//    fun set(value: T) {
//        this.value = value
//        dirty = true
//        valueChanged.trigger(value)
//    }
//}
fun <T> Flow<T>.asMutableState(scope: CoroutineScope, default: T): MutableStateValue<T> {
    val state = mutableStateOf(default)
    scope.launch {
        collect { state.set(it) }
    }
    return state
}
