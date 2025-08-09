package me.dvyy.particles.ui.helpers

import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.toString
import kotlin.math.pow
import kotlin.math.roundToInt

object UiSizes {
    val hGap: Dp get() = Dp(2f)
    val vGap: Dp get() = Dp(2f)

    val baseSize: Dp get() = Dp(4f)
    val menuWidth: Dp get() = baseSize * 7f
}

fun UiScope.MenuRow(vGap: Dp = UiSizes.vGap, block: UiScope.() -> Unit) {
    Row(width = Grow.Std) {
        modifier.margin(horizontal = UiSizes.hGap, vertical = vGap)
        block()
    }
}

fun UiScope.MenuTextInput(
    value: Float,
    min: Float = 0.0f,
    max: Float,
    precision: Int = 2,
    txtFormat: (Float) -> String = { it.toString(precision) },
    onChange: (Float) -> Unit,
    block: TextFieldScope.() -> Unit = {}
) {
    val tempValue = remember { mutableStateOf(txtFormat(value)) }
    val prevValue = remember { mutableStateOf(value) }
    if (prevValue.value != value) {
        prevValue.set(value)
        tempValue.set(txtFormat(value))
    }

    val round = 10.0.pow(precision)
    fun boundedChange(value: Float) {
        onChange(((value.coerceAtLeast(min) * round).roundToInt() / round).toFloat())
    }

    fun UiModifier.scrollable() = onWheelY {
        val multiplier = if (KeyboardInput.isShiftDown) 1f else 10f
        boundedChange(value + multiplier * (0.1f).pow(precision) * it.pointer.scroll.y)
    }

    TextField(tempValue.use()) {
        modifier
            .onChange { tempValue.set(it) }
            .onEnterPressed {
                tempValue.value.toFloatOrNull()?.let { boundedChange(it) }
                tempValue.set(txtFormat(value))
            }
            .align(yAlignment = AlignmentY.Center)
            .padding(vertical = sizes.smallGap * 0.5f)
            .scrollable()
        block()
    }
}
fun UiScope.MenuSlider2(
    label: String,
    value: Float,
    min: Float = 0.0f,
    max: Float,
    precision: Int = 2,
    txtFormat: (Float) -> String = { it.toString(precision) },
    onChange: (Float) -> Unit,
    block: SliderScope.() -> Unit = {},
//    sliderShown: Boolean = true,
) {
    val round = 10.0.pow(precision)
    fun boundedChange(value: Float) {
        onChange(((value.coerceAtLeast(min) * round).roundToInt() / round).toFloat())
    }

    fun UiModifier.scrollable() = onWheelY {
        val multiplier = if (KeyboardInput.isShiftDown) 1f else 10f
        boundedChange(value + multiplier * (0.1f).pow(precision) * it.pointer.scroll.y)
    }

//    MenuRow {
//        Text(label) { labelStyle(Grow.Std) }
//        MenuTextInput(value, min, max, precision, txtFormat, onChange)
//    }
    MenuRow {
        Slider(value, min, max) {
            modifier
                .width(Grow.Std)
                .alignY(AlignmentY.Center)
                .onChange { boundedChange(it) }
            block()
        }
        modifier.scrollable()
    }
}

fun TextScope.labelStyle(width: Dimension = FitContent) {
    modifier
        .width(width)
        .align(yAlignment = AlignmentY.Center)
        .padding(vertical = sizes.smallGap * 0.5f)
}
