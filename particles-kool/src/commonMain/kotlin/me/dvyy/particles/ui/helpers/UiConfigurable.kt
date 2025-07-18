package me.dvyy.particles.ui.helpers

import de.fabmax.kool.modules.ui2.Grow
import de.fabmax.kool.modules.ui2.Text
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.width
import me.dvyy.particles.config.UniformParameter
import me.dvyy.particles.ui.windows.TextInputWithTooltip

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
            MenuRow {
                modifier.width(Grow.Std)
                Text(name) { modifier.width(Grow.Std) }
                TextInputWithTooltip(
                    UniformParameter(name, "", value.toFloat()),
                    onChange = { onChange(it) }
                )
            }
//            MenuSlider2(
//                name,
//                value.toFloat(),
//                min = min,
//                max = max,
//                precision = precision,
//                onChange = onChange,
//            )
        }
    }

    data class Toggle(
        val label: String,
        val value: Boolean,
        val onChange: (Boolean) -> Unit,
    ) : UiConfigurable {
        override fun UiScope.draw() {
            LabeledSwitch(label, value, onChange)
        }
    }
}
