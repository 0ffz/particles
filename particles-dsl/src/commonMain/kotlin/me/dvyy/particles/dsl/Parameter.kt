package me.dvyy.particles.dsl

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Parameter.Serializer::class)
sealed interface Parameter<T> {
    data class Value<T>(val value: T) : Parameter<T>
    data class FromParams<T>(
        val path: String,
        val serializer: KSerializer<T>,
        val default: T,
    ) : Parameter<T>

    class Serializer<T>(val dataSerializer: KSerializer<T>) : KSerializer<Parameter<T>> {
        @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor = YamlNode.serializer().descriptor

        override fun deserialize(decoder: Decoder): Parameter<T> {
            val node = decoder.decodeSerializableValue(YamlNode.Companion.serializer())
            return if (node is YamlTaggedNode && node.tag == "!param") {
                FromParams<T>(
                    node.path.toHumanReadableString(),
                    dataSerializer,
                    Yaml.Companion.default.decodeFromYamlNode(dataSerializer, node.innerNode)
                )
            } else {
                Value<T>(Yaml.Companion.default.decodeFromYamlNode(dataSerializer, node))
            }
        }

        override fun serialize(encoder: Encoder, value: Parameter<T>) {
            TODO("Not yet implemented")
        }
    }
}
