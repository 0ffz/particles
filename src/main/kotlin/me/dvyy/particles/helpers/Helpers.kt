package me.dvyy.particles.helpers

import org.openrndr.draw.ComputeShader
import java.nio.file.Path
import kotlin.io.path.div

object Helpers {
    private val templateRegex = Regex("// ?\\{\\{([^}]+)}}")

    fun resourceStream(path: Path) = this::class.java.getResourceAsStream(path.joinToString(prefix = "/", separator = "/"))?.bufferedReader()
        ?: error("Resource not found: $path")

    fun computeShader(
        path: Path,
        name: String,
        templates: Map<String, String> = mapOf(),
    ): ComputeShader {
        val resource = resourceStream(path)
        val templatedLines = resource.lineSequence().map { line ->
            when {
                line.startsWith("#include") -> resourceStream(path.parent / line.split(" ")[1].removeSurrounding("\"")).readText()
                line.contains("//{{") -> line.replace(templateRegex) {
                    val key = it.groupValues[1].trim().removeSuffix(";")
                    templates[key] ?: error("Template not found: $key")
                }

                else -> line
            }
        }.joinToString("\n")
//        println(templatedLines)
        return ComputeShader.fromCode(templatedLines, name)
    }
}
