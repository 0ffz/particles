package helpers

import org.openrndr.draw.ComputeShader
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.div
import kotlin.io.path.readText

object Helpers {
    private val templateRegex = Regex("// ?\\{\\{([^}]+)}}")

    fun computeShader(
        path: Path,
        name: String,
        templates: Map<String, String> = mapOf()
    ): ComputeShader {
        val templatedLines = path.toFile().useLines { lines ->
            lines
                .map { line ->
                    when {
                        line.startsWith("#include") -> (path.parent / line.split(" ")[1].removeSurrounding("\"")).readText()
                        line.contains("//{{") -> line.replace(templateRegex) {
                            val key = it.groupValues[1].trim().removeSuffix(";")
                            templates[key] ?: error("Template not found: $key")
                        }
                        else -> line
                    }
                }
                .joinToString("\n")
        }
        println(templatedLines)
        return ComputeShader.fromCode(templatedLines, name)
    }
}
