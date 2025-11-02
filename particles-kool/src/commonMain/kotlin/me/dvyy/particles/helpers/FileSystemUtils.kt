package me.dvyy.particles.helpers

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import kotlinx.io.files.Path

expect object FileSystemUtils {
    fun read(path: ConfigPath): String?
    fun write(path: ConfigPath, content: String)
    fun clearCachedFileIfExists(path: ConfigPath)

    fun getPathOrNull(file: PlatformFile): Path?

    suspend fun saveFileAs(bytes: ByteArray, name: String)
//    fun toFileOrNull(path: Path): PlatformFile?

}

object FilePicker {
    suspend fun pickFile(vararg extensions: String): FilePickerResult? {
        val result = FileKit.openFilePicker(type = FileKitType.File(extensions.toSet())) ?: return null
        return FilePickerResult(
            ConfigPath(FileSystemUtils.getPathOrNull(result)?.toString() ?: result.name),
            result.readString(),
        )
    }
}

data class ConfigPath(
    val path: String,
) {
    val name get() = path.substringAfterLast("/")

    fun readContents(): FilePickerResult? {
        return FilePickerResult(this, FileSystemUtils.read(this) ?: return null)
    }

    override fun toString(): String = path
}

data class FilePickerResult(
    val path: ConfigPath,
    val contents: String,
)