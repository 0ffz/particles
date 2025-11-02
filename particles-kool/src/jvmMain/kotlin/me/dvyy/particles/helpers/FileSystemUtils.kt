package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.io.files.Path
import kotlin.io.path.*

actual object FileSystemUtils {
    actual fun read(path: ConfigPath): String? {
        val path = kotlin.io.path.Path(path.path)
        if (path.notExists()) return null
        return runCatching { path.readText() }.onFailure { it.printStackTrace() }.getOrNull()
    }

    actual fun write(path: ConfigPath, content: String) {
        kotlin.io.path.Path(path.path).createParentDirectories()
            .also { if (it.notExists()) it.createFile() }.writeText(content)
    }

    actual fun getPathOrNull(file: PlatformFile): Path? {
        return file.toKotlinxIoPath()
    }

    actual suspend fun saveFileAs(bytes: ByteArray, name: String) {
    }

    actual fun clearCachedFileIfExists(path: ConfigPath) {
    }
}
