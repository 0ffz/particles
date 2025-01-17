import dsl.ParticlesDSL

val ParticlesDSL.PairwisePotentials get() = PairwisePotentials(this)

class PairwisePotentials(val config: ParticlesDSL) {
    fun lennardJones() = config.function(
        "lennardJones", """
        float inv_r = sigma / dist;
        float inv_r6 = inv_r * inv_r * inv_r * inv_r * inv_r * inv_r;
        float inv_r12 = inv_r6 * inv_r6;
        return min(24.0 * epsilon * (2.0 * inv_r12 - inv_r6) / dist, maxForce);
        """.trimIndent()
    )
}
