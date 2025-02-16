package me.dvyy.particles

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

actual object FileSystemUtils {
    actual fun read(path: String): String {
        return Path(path).readText()
    }

    actual fun write(path: String, content: String) {
        Path(path).writeText(content)
    }
}
