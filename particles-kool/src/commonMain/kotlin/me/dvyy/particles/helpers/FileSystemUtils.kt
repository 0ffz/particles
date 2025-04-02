package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.files.Path

expect object FileSystemUtils {
    fun read(path: String): String?

    fun write(path: String, content: String)

    fun getPathOrNull(file: PlatformFile): Path?

    fun toFileOrNull(path: Path): PlatformFile?
}
