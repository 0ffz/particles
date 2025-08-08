package me.dvyy.particles.ui.helpers

import androidx.compose.runtime.*
import org.koin.core.scope.Scope

val LocalKoinScope = staticCompositionLocalOf<Scope> { error("Koin scope not provided") }

@Composable
inline fun <reified T : Any> koinInject(): T {
    val koinApplication = currentKoinApplication()
    return koinApplication.get<T>()
}

@OptIn(InternalComposeApi::class)
@Composable
fun currentKoinApplication() = currentComposer.run {
    remember { consume(LocalKoinScope) }
}
