package me.dvyy.particles.potentials

import me.dvyy.particles.dsl.PairwiseFunction

object PairwisePotentials {
    object LennardJones : PairwiseFunction(
        name = "lennardJones",
        body = """
            float inv_r = sigma / dist;
            float inv_r6 = inv_r * inv_r * inv_r * inv_r * inv_r * inv_r;
            float inv_r12 = inv_r6 * inv_r6;
            return min(24.0 * epsilon * (2.0 * inv_r12 - inv_r6) / dist, maxForce);
        """.trimIndent(),
    ) {
        val sigma = parameter("float", "sigma")
        val epsilon = parameter("float", "epsilon")
    }
}
