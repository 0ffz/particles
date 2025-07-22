package me.dvyy.particles.compute

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.pipeline.StorageBuffer
import de.fabmax.kool.util.*
import me.dvyy.particles.clustering.ClusterInfo
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.dsl.Particle
import me.dvyy.particles.helpers.Buffers
import me.dvyy.particles.helpers.init

class ParticleBuffers(
    private val configRepo: ConfigRepository,
) : Releasable {
    private val count = configRepo.count
    private val particleTypes: List<Particle> = configRepo.config.value.particles

    //    val positionBuffer = Buffers.positions(count, configRepo.boxSize)
//    val velocitiesBuffer = Buffers.velocities(count, configRepo.config.value.simulation.threeDimensions, configRepo.config.value.simulation.maxVelocity)
//    val forcesBuffer = StorageBuffer(GpuType.Float4, count).initFloat4 { Vec4f.ZERO }
//    val particleGridCellKeys = Buffers.integers(count)
//    val sortIndices = Buffers.integers(count)
    val particleBuffer = StorageBuffer(ParticleStruct().type, count)
    val particleTypeBuffer = StorageBuffer(ParticleTypeStruct().type, particleTypes.size)
    val offsetsBuffer = Buffers.integers(count)

    /** Integer id for the particle's current cluster, or max value if an outlier. */
    val clustersBuffer = Buffers.integers(count)
    var clusterInfo: ClusterInfo? = null


    init {
        initializeParticlesBuffer()
        initializeParticleTypesBuffer()
    }

    fun initializeParticleTypesBuffer() {
        val typesBuffer = StructBuffer(particleTypes.size, ParticleTypeStruct()).apply {
            particleTypes.forEach { type ->
                put {
                    radius.set(type.radius.toFloat())
                    color.set(Color(type.color).toVec4f())
                }
            }
        }
        particleTypeBuffer.uploadData(typesBuffer)
    }

    fun initializeParticlesBuffer() {
        val sim = configRepo.config.value.simulation
        val particles = StructBuffer(count, ParticleStruct()).init {
            position.set(Buffers.randomPosition(configRepo.boxSize))
            velocity.set(Buffers.randomVelocity(sim.threeDimensions, sim.maxVelocity))
            force.set(Vec3f.ZERO)
        }

        // Set particle types based on distribution, ensuring we always get exactly `count` particles
        if (!particleTypes.isEmpty()) {
            val distTotal = particleTypes.sumOf { it.distribution }
            val counts = particleTypes.map { ((it.distribution / distTotal) * count).toInt() }

            var type = 0
            var offset = 0
            repeat(count) {
                if (it - offset >= counts[type] && type != counts.lastIndex) {
                    type++
                    offset = it
                }

                particles[it].particleType.set(type)
            }
        }

        particleBuffer.uploadData(particles)
    }

    override var isReleased: Boolean = false

    override fun release() {
        isReleased = true
        particleBuffer.release()
        particleTypeBuffer.release()
        offsetsBuffer.release()
    }
}

/**
 * Struct representing data for a single particle
 */
class ParticleStruct : Struct("ParticleStruct", MemoryLayout.Std140) {
    val position = float3("position")
    val velocity = float3("velocity")
    val force = float3("force")
    val particleType = int1("particleType")
    val gridCellId = int1("gridCellId")
}

/**
 * Struct representing a type of particle and its appearance.
 */
class ParticleTypeStruct : Struct("ParticleTypeStruct", MemoryLayout.Std140) {
    val color = float4("color")
    val radius = float1("radius")
}
