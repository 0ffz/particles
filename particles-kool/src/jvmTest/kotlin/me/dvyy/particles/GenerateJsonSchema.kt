package me.dvyy.particles

import io.github.smiley4.schemakenerator.core.CoreSteps.initial
import io.github.smiley4.schemakenerator.core.data.AnnotationData
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.compileInlining
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.generateJsonSchema
import io.github.smiley4.schemakenerator.jsonschema.jsonDsl.JsonObject
import io.github.smiley4.schemakenerator.jsonschema.jsonDsl.JsonTextValue
import io.github.smiley4.schemakenerator.serialization.SerializationSteps.analyzeTypeUsingKotlinxSerialization
import me.dvyy.particles.dsl.ParticlesConfig
import org.junit.jupiter.api.Test

class GenerateJsonSchema {
    @Test
    fun `generate schema`() {
        initial<ParticlesConfig>()
            .analyzeTypeUsingKotlinxSerialization()
            .generateJsonSchema {
                this.optionals = JsonSchemaSteps.RequiredHandling.NON_REQUIRED
            }
            .apply {
                this.entries.forEach {
                    val description = determineDescription(it.typeData.annotations)
                    if(description != null) {
                        ((it.json as? JsonObject) ?: return@forEach).properties["description"] = JsonTextValue(description)
                    }
                }
            }
            .compileInlining()
            .json
            .prettyPrint()
            .let { println(it) }
    }

    private fun determineDescription(annotations: Collection<AnnotationData>): String? {
        return annotations
            .filter { it.name == me.dvyy.particles.dsl.annotations.Description::class.qualifiedName }
            .map { it.values["description"] as String }
            .firstOrNull()
    }
}
