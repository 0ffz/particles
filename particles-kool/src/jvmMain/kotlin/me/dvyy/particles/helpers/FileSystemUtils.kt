package me.dvyy.particles.helpers

import kotlin.io.path.*

actual object FileSystemUtils {
    actual fun read(path: String): String? {
        val path = Path(path)
        if (path.notExists()) return null
        return runCatching { path.readText() }.onFailure { it.printStackTrace() }.getOrNull()
    }

    actual fun write(path: String, content: String) {
        Path(path).createParentDirectories().also { if (it.notExists()) it.createFile() }.writeText(content)
    }
}
