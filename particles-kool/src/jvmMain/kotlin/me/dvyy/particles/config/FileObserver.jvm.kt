package me.dvyy.particles.config

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.toKotlinxIoPath
import io.github.vinceglb.filekit.utils.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile

actual fun PlatformFile.observeChanges(): Flow<PlatformFile> = callbackFlow {
    val path = this@observeChanges.toKotlinxIoPath().toFile().toPath().absolute()
    if (!path.isRegularFile()) return@callbackFlow

    val directory = path.parent
    val fileName = path.fileName

    val watchService = FileSystems.getDefault().newWatchService()

    try {
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )
    } catch (e: Exception) {
        watchService.close()
        throw e
    }

    val job = launch(Dispatchers.IO) {
        while (true) {
            val watchKey = withContext(Dispatchers.IO) {
                watchService.take()
            }

            val hasChanges = watchKey.pollEvents().any { event ->
                val eventPath = event.context() as Path
                eventPath.fileName == fileName
            }

            if (hasChanges) {
                trySend(this@observeChanges)
            }

            if (!watchKey.reset()) {
                break
            }
        }
    }

    awaitClose {
        job.cancel()
        watchService.close()
    }
}
