package me.dvyy.particles.ui

import de.fabmax.kool.ImageBitmapOptions
import de.fabmax.kool.createImageBitmap
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.ImageData2d
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.platform.ImageTextureData
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.Image
import org.w3c.dom.ImageBitmap

actual suspend fun loadSvg(svg: String): Result<ImageData2d> {
    return loadImageBitmap(svg, null).map { ImageTextureData(it, "", TexFormat.RGBA) }
}

private suspend fun loadImageBitmap(svgString: String, resize: Vec2i?): Result<ImageBitmap> {
    val deferredBitmap = CompletableDeferred<ImageBitmap>()
    val img = resize?.let { Image(it.x, it.y) } ?: Image()
    img.onload = {
        createImageBitmap(img, ImageBitmapOptions(resize)).then { bmp -> deferredBitmap.complete(bmp) }
    }
    img.onerror = { _, _, _, _, _ ->
        deferredBitmap.completeExceptionally(IllegalStateException("Failed loading tex from $svgString"))
    }
    img.textContent
    img.crossOrigin = ""
    img.src = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svgString)
    return try {
        Result.success(deferredBitmap.await())
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

external fun encodeURIComponent(uriComponent: String): String
