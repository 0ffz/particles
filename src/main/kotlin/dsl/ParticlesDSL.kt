package dsl

import FieldsApplication
import io.ktor.server.config.yaml.*
import org.openrndr.color.ColorRGBa

@DslMarker
annotation class ParticlesDSLMarker

data class ParticlesConfiguration(
    val particleTypes: List<ParticleType>,
    val interactions: List<PairInteractions>,
    val functions: List<PairwiseFunction>,
    val parameters: YamlConfig,
)

class ParticlesDSL {
    private val config = YamlConfig("_configuration/parameters.yaml") ?: error("Configuration file not found")

    //    private val functions = mutableListOf<PairwiseFunction>()
    private val particleTypes = mutableListOf<ParticleType>()
    private val interactions: ParticleInteractions = ParticleInteractions(particleTypes)

    fun config(key: String, default: Any? = null): String =
        config.propertyOrNull(key)?.getString() ?: default?.toString() ?: error("Key $key not found in config")

    fun particle(
        name: String,
        color: ColorRGBa,
        radius: Double,
    ): ParticleType {
        return ParticleType(
            name = name,
            color = color,
            radius = radius,
            id = particleTypes.size.toUInt(),
        ).also { particleTypes.add(it) }
    }

    fun interactions(block: ParticleInteractions.() -> Unit) {
        interactions.block()
    }

//    fun <T> function(
//        name: String,
//        @Language(
//            "glsl", prefix = """
//float sigma;
//float epsilon;
//float func(float dist) {
//""", suffix = "}"
//        ) body: String,
//        parameters: T,
//    ): PairwiseFunction {
//        return PairwiseFunction(name = name, body = body, parameters = parameters.map {
//            val (type, name) = it.split(" ")
//            InteractionParameter(name = name, type = type)
//        }).also { functions.add(it) }
//    }

    fun build() = ParticlesConfiguration(
        particleTypes = particleTypes,
        interactions = interactions.pairInteractions,
        functions = interactions.pairInteractions.flatMap { it.functions.map { it.function } }.distinct(),
        parameters = config,
    )

    fun start() {
        FieldsApplication(build()).start()
    }
}

@ParticlesDSLMarker
fun particles(block: ParticlesDSL.() -> Unit) {
    ParticlesDSL().apply(block).start()
}

