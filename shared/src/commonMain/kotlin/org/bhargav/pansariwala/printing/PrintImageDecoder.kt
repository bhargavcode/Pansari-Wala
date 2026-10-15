package org.bhargav.pansariwala.printing

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.printer.model.ArgbImage
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Decodes PNG / JPEG bytes and downsizes them to at most [maxWidth] pixels so large photos never
 * stay in memory at full resolution.
 */
suspend fun decodePrintImage(bytes: ByteArray, maxWidth: Int): ArgbImage? = withContext(Dispatchers.Default) {
    runCatching {
        val source = bytes.decodeToImageBitmap()
        val width = minOf(source.width, maxWidth).coerceAtLeast(1)
        val height = (source.height.toLong() * width / source.width).toInt().coerceAtLeast(1)
        val scaled = if (width == source.width) {
            source
        } else {
            ImageBitmap(width, height).also { target ->
                Canvas(target).drawImageRect(
                    image = source,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(source.width, source.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width, height),
                    paint = Paint(),
                )
            }
        }
        val pixels = IntArray(width * height)
        scaled.readPixels(pixels, 0, 0, width, height)
        ArgbImage(width, height, pixels)
    }.getOrNull()
}
