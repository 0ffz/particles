package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.io.files.Path
import kotlin.io.path.*

actual object FileSystemUtils {
    actual fun read(path: String): String? {
        val path = kotlin.io.path.Path(path)
        if (path.notExists()) return null
        return runCatching { path.readText() }.onFailure { it.printStackTrace() }.getOrNull()
    }

    actual fun write(path: String, content: String) {
        kotlin.io.path.Path(path).createParentDirectories()
            .also { if (it.notExists()) it.createFile() }.writeText(content)
    }

    actual fun getPathOrNull(file: PlatformFile): Path? {
        return file.toKotlinxIoPath()
    }
    actual fun toFileOrNull(path: Path): PlatformFile? {
        return PlatformFile(path)
    }
}
