package dsl

import FieldsApplication
import org.intellij.lang.annotations.Language
import org.openrndr.color.ColorRGBa

@DslMarker
annotation class ParticlesDSLMarker

data class ParticlesConfiguration(
    val particleTypes: List<ParticleType>,
    val interactions: List<ParticleInteractions>,
    val functions: List<InteractionFunction>,
)

class ParticlesDSL {
    private val functions = mutableListOf<InteractionFunction>()
    private val interactions = mutableListOf<ParticleInteractions>()
    private val particleTypes = mutableListOf<ParticleType>()

    fun particle(
        color: ColorRGBa,
        radius: Double,
    ): ParticleType {
        return ParticleType(
            color = color,
            radius = radius,
            id = particleTypes.size.toUInt(),
        ).also { particleTypes.add(it) }
    }

    infix fun Pair<ParticleType, ParticleType>.interaction(block: ParticleInteractions.() -> Unit) {
        ParticleInteractions(this).apply(block).also { interactions.add(it) }
    }

    fun function(
        name: String,
        @Language(
            "glsl", prefix = """
float sigma;
float epsilon;
float func(float dist) {
""", suffix = "}"
        ) body: String,
    ): InteractionFunction {
        return InteractionFunction(name = name, body = body).also { functions.add(it) }
    }

    fun build() = ParticlesConfiguration(
        particleTypes = particleTypes,
        interactions = interactions,
        functions = functions,
    )
    fun start() {
        FieldsApplication(build()).start()
    }
}

@ParticlesDSLMarker
fun particles(block: ParticlesDSL.() -> Unit) {
    ParticlesDSL().apply(block).start()
}

