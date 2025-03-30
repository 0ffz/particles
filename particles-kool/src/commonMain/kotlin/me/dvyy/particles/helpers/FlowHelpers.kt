package me.dvyy.particles.helpers

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Collect this flow as a [de.fabmax.kool.modules.ui2.MutableState] usable in Kool's UI system.
 */
fun <T> Flow<T>.asMutableState(scope: CoroutineScope, default: T): MutableStateValue<T> {
    val state = mutableStateOf(default)
    scope.launch {
        collect { state.set(it) }
    }
    return state
}

fun <T> StateFlow<T>.asMutableState(scope: CoroutineScope): MutableStateValue<T> {
    return asMutableState(scope, this.value)
}
