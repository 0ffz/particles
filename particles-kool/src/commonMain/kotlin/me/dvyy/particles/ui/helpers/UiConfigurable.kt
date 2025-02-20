package me.dvyy.particles.ui.helpers

import de.fabmax.kool.modules.ui2.UiScope

sealed interface UiConfigurable {
    fun UiScope.draw()
    data class Slider<T : Number>(
        val name: String,
        val value: T,
        val min: Float = 0f,
        val max: Float = 100f,
        val precision: Int = 2,
        val onChange: (Float) -> Unit,
    ) : UiConfigurable {
        override fun UiScope.draw() {
            MenuSlider2(
                name,
                value.toFloat(),
                min = min,
                max = max,
                precision = precision,
                onChange = onChange,
            )
        }
    }

    data class Toggle(
        val label: String,
        val value: Boolean,
        val onChange: (Boolean) -> Unit,
    ): UiConfigurable {
        override fun UiScope.draw() {
            LabeledSwitch(label, value, onChange)
        }
    }
}
