package me.dvyy.particles

import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

actual object FileSystemUtils {
    actual fun read(path: String): String {
        return Path(path).inputStream().bufferedReader().readText()
    }

    actual fun write(path: String, content: String) {
        Path(path).outputStream().bufferedWriter().write(content)
    }
}
