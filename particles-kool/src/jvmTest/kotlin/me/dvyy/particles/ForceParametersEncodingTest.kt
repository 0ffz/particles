package me.dvyy.particles

import de.fabmax.kool.math.Mat4f
import me.dvyy.particles.compute.forces.ForceWithParameters
import me.dvyy.particles.dsl.ParticleId
import me.dvyy.particles.dsl.pairwise.ParticlePair
import me.dvyy.particles.helpers.TestForce
import org.junit.Test
import kotlin.test.assertEquals

class ForceParametersEncodingTest {
    private val force = ForceWithParameters(TestForce, totalParticles = 1)
    val pairAA = ParticlePair.of(ParticleId(0), ParticleId(0), 1)
    @Test
    fun `put should populate columns, then rows`() {
        force.put(pairAA, floatArrayOf(1f, 2f, 3f, 4f, 5f))
        assertEquals(force.get(pairAA), Mat4f(
            1f, 4f, 0f, 0f,
            1f, 5f, 0f, 0f,
            2f, 0f, 0f, 0f,
            3f, 0f, 0f, 0f,
        ))
    }
}
