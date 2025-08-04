package me.dvyy.particles.render

import de.fabmax.kool.modules.ksl.lang.*
import kotlinx.serialization.Serializable

/**
 * These functions output a color based on an input value from 0 to 1
 */
@Serializable
enum class Gradients() {
    /** Color from blue, to orange, to white */
    HEAT {
        context(builder: KslScopeBuilder)
        override fun recolor(input: KslExprFloat1) = with(builder) {
            mix(
                mix(
                    // Dark blue
                    float4Value(0f, 0f, 1f, 1f),
                    // Orange
                    float4Value(1f, 0.5f, 0f, 1f), input * 2f.const
                ),
                // White
                float4Value(1f, 1f, 1f, 1f),
                max(0f.const, input * 2f.const - 1f.const)
            )
        }
    },

    /** Black and white. */
    MONOCHROMATIC {
        context(builder: KslScopeBuilder)
        override fun recolor(input: KslExprFloat1) = with(builder) {
            mix(
                float4Value(0f, 0f, 0f, 1f),
                float4Value(1f, 1f, 1f, 1f),
                input
            )
        }
    };

    context(builder: KslScopeBuilder)
    abstract fun recolor(input: KslExprFloat1): KslExprFloat4
}
