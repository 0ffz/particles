package me.dvyy.particles.helpers

actual object FileSystemUtils {
    actual fun read(path: String): String? = """
        simulation:
          threeDimensions: true
          size:
            width: 500
            height: 500
            depth: 50
          count: 10000
          dT: "0.001"
          maxForce: "50000.0"
          maxVelocity: "20.0"
          epsilon: 100
    """.trimIndent()

    actual fun write(path: String, content: String) {

    }
}
