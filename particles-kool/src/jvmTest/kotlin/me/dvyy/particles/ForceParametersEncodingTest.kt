package me.dvyy.particles

import de.fabmax.kool.math.Mat4f
import me.dvyy.particles.compute.forces.ForceWithParameters
import me.dvyy.particles.dsl.Particle
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.dsl.pairwise.ParticleSet
import me.dvyy.particles.helpers.TestForce
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ForceParametersEncodingTest {
    private val force = ForceWithParameters(TestForce, totalParticles = 1)
    val config = ParticlesConfig(nameToParticle = mapOf(
        "a" to Particle()
    ))
    val pairAA = with(config) {
        ParticleSet.of(particle("a"), particle("a"))
    }

    val encodedMatrix = Mat4f(
        1f, 4f, 0f, 0f,
        1f, 5f, 0f, 0f,
        2f, 0f, 0f, 0f,
        3f, 0f, 0f, 0f,
    )

    @Test
    fun `put should populate columns, then rows`() {
        force.put(pairAA, floatArrayOf(1f, 2f, 3f, 4f, 5f))
        assertEquals(force.get(pairAA), encodedMatrix)
    }
    @Test
    fun `should correctly extract parameters from matrix`() {
        assertContentEquals(
            floatArrayOf(1f, 2f, 3f, 4f, 5f),
            ForceWithParameters.toArray(encodedMatrix, 5),
        )
    }
}
