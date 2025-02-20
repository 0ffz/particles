package me.dvyy.particles.config

import de.fabmax.kool.math.Vec3i
import de.fabmax.kool.math.toVec3f
import de.fabmax.kool.math.toVec3i
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.delayFrames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.dvyy.particles.compute.WORK_GROUP_SIZE
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.helpers.FileSystemUtils
import kotlin.math.sqrt

class ConfigRepository {
    val paramsPath = "parameters.yml"
    val configPath = "particles.yml"
    private val appScope = CoroutineScope(Dispatchers.RenderLoop)
    val parameters = YamlParameters(path = paramsPath, scope = appScope)

    private val _config = MutableStateFlow(ParticlesConfig())
    val config = _config.asStateFlow()
    private val _configLines = MutableStateFlow("")
    val configLines = _configLines.asStateFlow()

    var isDirty: Boolean = true
        private set

    val count get() = (_config.value.simulation.targetCount / WORK_GROUP_SIZE) * WORK_GROUP_SIZE
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
                sqrt((desiredSize.x.toFloat() * desiredSize.y.toFloat() * (desiredSize.z.coerceAtLeast(1)).toFloat()) / count) + 1.0
            } else smallestSize
        }.toFloat()

    val gridCells get() = desiredSize.toVec3f().div(gridSize).toVec3i()
    val boxSize get() = gridCells.toVec3f().times(gridSize)

    fun loadConfig() {
        val configLines = FileSystemUtils.read(configPath) ?: "{}"
        _configLines.update { configLines }
        runCatching { YamlHelpers.yaml.decodeFromString(ParticlesConfig.serializer(), configLines) }
            .onSuccess { updateConfig(it) }
        parameters.load()
    }

    fun updateConfig(config: ParticlesConfig) {
        _config.update { config }
    }

    fun saveConfig() {
//        val config = YamlHelpers.yaml.encodeToString(ParticlesConfig.serializer(), _config.value)
//        FileSystemUtils.write(configPath, config)
        parameters.save()
    }

    fun saveConfigLines(newLines: String) {
        FileSystemUtils.write(configPath, newLines)
        _configLines.update { newLines }
    }

    inline fun whenDirty(run: ParticlesConfig.() -> Unit) {
        if (isDirty) run(config.value)
    }

    init {
        loadConfig()
        appScope.launch {
            isDirty = false
            config.collect {
                if (!isDirty) {
                    isDirty = true
                    delayFrames(1)
                    isDirty = false
                }
            }
        }
    }
}
