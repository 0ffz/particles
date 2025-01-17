package dsl

data class InteractionFunction(
    val name: String,
    val body: String,
) {
    fun render() = """
        float $name(float dist) {
            $body
        }
    """.trimIndent()
}
