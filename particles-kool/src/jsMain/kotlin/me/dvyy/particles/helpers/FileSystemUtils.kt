package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.files.Path

actual object FileSystemUtils {
    actual fun read(path: String): String? = null

    actual fun write(path: String, content: String) = Unit

    actual fun getPathOrNull(file: PlatformFile): Path? = null

    actual fun toFileOrNull(path: Path): PlatformFile? = null
}
