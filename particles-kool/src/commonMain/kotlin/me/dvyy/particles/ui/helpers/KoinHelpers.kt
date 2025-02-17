package me.dvyy.particles.ui.helpers

import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.remember
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.mp.KoinPlatformTools

inline fun <reified T: Any> UiScope.inject() =
    remember<T> { KoinPlatformTools.defaultContext().get().get<T>() }
