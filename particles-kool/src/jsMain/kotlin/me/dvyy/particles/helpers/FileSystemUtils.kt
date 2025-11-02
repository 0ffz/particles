package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.download
import kotlinx.browser.localStorage
import kotlinx.io.files.Path
import org.w3c.dom.get
import org.w3c.dom.set

actual object FileSystemUtils {
    actual fun read(path: ConfigPath): String? {
        return localStorage["paths/$path"]
    }

    actual fun write(path: ConfigPath, content: String) {
        localStorage["paths/$path"] = content
    }

    actual fun getPathOrNull(file: PlatformFile): Path? = null

    actual suspend fun saveFileAs(bytes: ByteArray, name: String) {
        FileKit.download(bytes, name)
    }

    actual fun clearCachedFileIfExists(path: ConfigPath) {
        localStorage.removeItem("paths/$path")
    }
}
