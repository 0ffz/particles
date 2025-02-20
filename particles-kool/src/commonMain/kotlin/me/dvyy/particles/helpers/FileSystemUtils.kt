package me.dvyy.particles.helpers

expect object FileSystemUtils {
    fun read(path: String): String?

    fun write(path: String, content: String)
}
