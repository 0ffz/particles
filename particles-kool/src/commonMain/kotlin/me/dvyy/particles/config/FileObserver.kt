package me.dvyy.particles.config

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow


expect fun PlatformFile.observeChanges(): Flow<PlatformFile>
