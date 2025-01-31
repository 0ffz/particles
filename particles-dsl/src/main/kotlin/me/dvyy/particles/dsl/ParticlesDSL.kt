package me.dvyy.particles.dsl

import org.openrndr.color.ColorRGBa
import kotlin.io.path.Path
import kotlin.io.path.inputStream

@DslMarker
annotation class ParticlesDSLMarker

data class ParticlesConfiguration(
    val particleTypes: List<ParticleType>,
    val interactions: List<PairInteractions>,
    val functions: List<PairwiseFunction>,
    val parameters: YamlConfig,
    val application: ApplicationConfiguration,
)

data class ApplicationConfiguration(
    var fullscreen: Boolean = false,
    var width: Int = 1280,
    var height: Int = 720,
)

class ParticlesDSL(
    configPath: String,
) {
    private val config = YamlConfig(Path(configPath).inputStream()) ?: error("Configuration file not found")

    //    private val functions = mutableListOf<PairwiseFunction>()
    private val particleTypes = mutableListOf<ParticleType>()
    private val interactions: ParticleInteractions = ParticleInteractions(particleTypes)
    val application = ApplicationConfiguration()

    fun config(key: String, default: Any? = null): String =
        config.propertyOrNull(key) ?: default?.toString() ?: error("Key $key not found in config")

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

    fun application(block: ApplicationConfiguration.() -> Unit) {
        application.block()
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
        application = application.copy(),
    )
}

@ParticlesDSLMarker
fun particles(
    config: String = "parameters.yml",
    block: ParticlesDSL.() -> Unit,
) = ParticlesDSL(config).apply(block)
