package me.dvyy.particles.ui.app

import de.fabmax.kool.Assets
import de.fabmax.kool.MimeType
import de.fabmax.kool.pipeline.ImageData2d
import de.fabmax.kool.util.toBuffer

actual suspend fun loadSvg(svg: String): Result<ImageData2d> {
    return runCatching {
        Assets.loadImageFromBuffer(
            svg.encodeToByteArray().toBuffer(),
            MimeType.IMAGE_SVG,
        )
    }
}
