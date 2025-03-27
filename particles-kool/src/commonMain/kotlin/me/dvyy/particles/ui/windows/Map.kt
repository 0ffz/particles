package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf

fun <T, R> MutableStateValue<T>.map(mapping: (T) -> R): MutableStateValue<R> {
    return mutableStateOf(mapping(this.value)).also {
        onChange { oldValue, newValue -> it.set(mapping(newValue)) }
    }
}
