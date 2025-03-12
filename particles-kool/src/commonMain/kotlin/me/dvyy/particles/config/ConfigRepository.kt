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
    /** Config as it was loaded */
    var initialConfig: ParticlesConfig? = _config.value
    val config = _config.asStateFlow()
    private val _configLines = MutableStateFlow("")
    val configLines = _configLines.asStateFlow()

    var isDirty: Boolean = true

    val count get() = (_config.value.simulation.targetCount / WORK_GROUP_SIZE) * WORK_GROUP_SIZE
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
                sqrt((desiredSize.x.toFloat() * desiredSize.y.toFloat() * (desiredSize.z.coerceAtLeast(1)).toFloat()) / count) + 1.0
            } else smallestSize
        }.toFloat()

    val gridCells get() = desiredSize.toVec3f().div(gridSize).toVec3i()
    val boxSize get() = gridCells.toVec3f().times(gridSize)

    fun loadConfig() {
        val configLines = FileSystemUtils.read(configPath)
            // Use default config if none exists
            ?: """
                simulation:
                  count: 10000
                  minGridSize: 5.0
                  dT: 0.001
                  conversionRate: 100
                  maxVelocity: 1.21
                  maxForce: 100000.0
                  threeDimensions: false
                  passesPerFrame: 100
                  size:
                    width: 1000
                    height: 1000
                    depth: 1000
                particles:
                  hydrogen:
                    color: ffffff
                    radius: 1.0
                    distribution: 2
                  oxygen:
                    color: ff0000
                    radius: 1.5
                    distribution: 1
                    #convertTo:
                      # type: hydrogen
                      # chance: 0.01
                #  hidden:
                #    color: 000000
                #    radius: 0
                #    distribution : 1
                interactions:
                  hydrogen-oxygen:
                    lennardJones:
                      sigma: !param;max=10 5.0
                      epsilon: !param;max=500 5.0
                  hydrogen-hydrogen:
                    lennardJones:
                      sigma: !param;max=10 5.0
                      epsilon: !param;max=500 5.0
                  oxygen-oxygen:
                    lennardJones:
                      sigma: !param;max=10 10.0
                      epsilon: !param;max=500 5.0
            """.trimIndent()
//            ?: YamlHelpers.yaml.encodeToString(ParticlesConfig.serializer(), ParticlesConfig())

        _configLines.update { configLines }
        runCatching { YamlHelpers.yaml.decodeFromString(ParticlesConfig.serializer(), configLines) }
            .onSuccess { updateConfig(it) }
        initialConfig = _config.value
        parameters.load()
    }

    //TODO seems to reset count incorrectly when hot-reloaded in config
    fun resetParameters() {
        parameters.reset()
        _config.update { initialConfig ?: it }
    }

    fun updateConfig(config: ParticlesConfig) {
        _config.update { config }
    }

    fun saveParameters() {
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
