package me.dvyy.particles.dsl.pairwise

import me.dvyy.particles.dsl.glsl.GLSLParameter

abstract class PairwiseFunction(
    val name: String,
//    @Language(
//        "glsl", prefix = """
//    float sigma;
//    float epsilon;
//    float func(float dist) {
//    """, suffix = "}"
//    )
    val body: String,
) {
    // TODO immutability - split into builder
    val parameters = mutableListOf<GLSLParameter>()
    fun parameter(type: String, name: String) = GLSLParameter(name, type).also { parameters.add(it) }

    fun render() = """
        float $name(float dist${
        if (parameters.isEmpty()) "" else parameters.joinToString(
            prefix = ", ",
            separator = ", "
        ) { "${it.type} ${it.name}" }
    }) {
            $body
        }
    """.trimIndent()
}
