package me.dvyy.particles.config

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun PlatformFile.observeChanges(): Flow<PlatformFile> = flow {
}
