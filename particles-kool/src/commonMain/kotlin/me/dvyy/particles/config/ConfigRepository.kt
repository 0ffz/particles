package me.dvyy.particles.config

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.math.toVec3f
import de.fabmax.kool.math.toVec3i
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.dvyy.particles.compute.partitioning.WORK_GROUP_SIZE
import me.dvyy.particles.dsl.Particle
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.Simulation
import me.dvyy.particles.helpers.FileSystemUtils
import kotlin.math.pow


class ConfigRepository(
    val settings: AppSettings,
) {
    private val _config = MutableStateFlow(
        ParticlesConfig(
            simulation = Simulation(count = 1000, maxVelocity = 1.0),
            nameToParticle = mapOf("argon" to Particle(color = "ff0000", radius = 2.0))
        )
    )
    private val _configLines =
        MutableStateFlow(YamlHelpers.yaml.encodeToString(ParticlesConfig.serializer(), _config.value))
    private val _currentFile = MutableStateFlow<PlatformFile?>(null)
    val config = _config.asStateFlow()
    val configLines = _configLines.asStateFlow()
    val currentFile = _currentFile.asStateFlow()

    var isDirty: Boolean = true

    val count
        get() = ((_config.value.simulation.count / WORK_GROUP_SIZE) * WORK_GROUP_SIZE).coerceAtLeast(
            WORK_GROUP_SIZE
        )
    val numGroups get() = Vec3i(count / WORK_GROUP_SIZE, 1, 1)

    private val desiredSize: Vec3i
        get() = run {
            val size = _config.value.simulation.size
            Vec3i(size.width, size.height, if (_config.value.simulation.threeDimensions) size.depth else 0)
        }

    val gridSize
        get() = run {
            val smallestSize = _config.value.simulation.minGridSize
            val cols = (desiredSize.x / smallestSize).toInt()
            val rows = (desiredSize.y / smallestSize).toInt()
            val depths = if (desiredSize.z == 0) 1 else (desiredSize.z / smallestSize).toInt()
            if (rows * cols * depths > count) {
                ((desiredSize.x.toFloat() * desiredSize.y.toFloat() * (desiredSize.z.coerceAtLeast(1)).toFloat()) / count)
                    .pow(if (desiredSize.z == 0) 1f / 2 else 1f / 3f)
            } else smallestSize
        }.toFloat()

    val gridCells get() = desiredSize.toVec3f().div(gridSize).toVec3i()
    val boxSize get() = gridCells.toVec3f().times(gridSize)

    fun loadConfig(configString: String) {
        _configLines.update { configString }
        runCatching { YamlHelpers.yaml.decodeFromString(ParticlesConfig.serializer(), configString) }
            .onSuccess { updateConfig(it) }
            .onFailure { it.printStackTrace() }
    }

    fun updateConfig(config: ParticlesConfig) {
        _config.update { config }
        isDirty = true
    }

    fun saveConfigLines(newLines: String) {
        val path = FileSystemUtils.getPathOrNull(_currentFile.value ?: return) ?: return
        FileSystemUtils.write(path.toString(), newLines)
        _configLines.update { newLines }
    }

    inline fun whenDirty(run: ParticlesConfig.() -> Unit) {
        if (isDirty) run(config.value)
    }

    suspend fun openFile(file: PlatformFile): Result<Unit> = runCatching {
        _currentFile.update { file }
        loadConfig(file.readString())
    }
}
