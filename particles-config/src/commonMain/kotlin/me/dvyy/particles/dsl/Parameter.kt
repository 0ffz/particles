package me.dvyy.particles.dsl

import com.charleskorn.kaml.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Parameter.Serializer::class)
sealed interface Parameter<T> {
    data class Value<T>(val value: T) : Parameter<T>
    data class FromParams<T>(
        val path: String,
        val serializer: KSerializer<T>,
        val default: T,
        val min: Double = 0.0,
        val max: Double = 100.0,
    ) : Parameter<T>

    class Serializer<T>(val dataSerializer: KSerializer<T>) : KSerializer<Parameter<T>> {
        @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor = YamlNode.serializer().descriptor

        override fun deserialize(decoder: Decoder): Parameter<T> {
            val node = decoder.decodeSerializableValue(YamlNode.Companion.serializer())
            return if (node is YamlTaggedNode && node.tag.startsWith("!param")) {
                val tags = node.tag.split(";").drop(1)
                val min = tags.firstOrNull { it.startsWith("min=") }?.removePrefix("min=")?.toDouble() ?: 0.0
                val max = tags.firstOrNull { it.startsWith("max=") }?.removePrefix("max=")?.toDouble() ?: 100.0
                FromParams<T>(
                    node.path.toHumanReadableString(),
                    dataSerializer,
                    Yaml.Companion.default.decodeFromYamlNode(dataSerializer, node.innerNode),
                    min, max
                )
            } else {
                Value<T>(Yaml.Companion.default.decodeFromYamlNode(dataSerializer, node))
            }
        }

        override fun serialize(encoder: Encoder, value: Parameter<T>) {
            encoder.encodeSerializableValue(
                YamlNode.serializer(), when (value) {
                    is FromParams -> YamlTaggedNode(
                        "!param;max=${value.max}",
                        YamlScalar(Yaml.default.encodeToString(dataSerializer, value.default), YamlPath.root)
                    )

                    is Value -> YamlScalar(Yaml.default.encodeToString(dataSerializer, value.value), YamlPath.root)
                }
            )
        }
    }
}
