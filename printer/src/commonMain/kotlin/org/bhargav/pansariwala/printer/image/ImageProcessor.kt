package org.bhargav.pansariwala.printer.image

import org.bhargav.pansariwala.printer.model.ArgbImage
import org.bhargav.pansariwala.printer.model.DitherMode
import org.bhargav.pansariwala.printer.model.MonoBitmap

/** Converts colour images into thermal-printer friendly 1-bit rasters. Pure Kotlin, no platform APIs. */
object ImageProcessor {

    fun toMonochrome(
        image: ArgbImage,
        targetWidth: Int,
        mode: DitherMode = DitherMode.FLOYD_STEINBERG,
        threshold: Int = 128,
        brightness: Int = 0,
        invert: Boolean = false,
    ): MonoBitmap {
        val width = targetWidth.coerceIn(8, maxOf(8, image.width * 4))
        val height = maxOf(1, (image.height.toLong() * width / image.width).toInt())
        val gray = scaleToGray(image, width, height, brightness, invert)
        val black = when (mode) {
            DitherMode.THRESHOLD -> BooleanArray(gray.size) { gray[it] < threshold.coerceIn(1, 254) }
            DitherMode.FLOYD_STEINBERG -> errorDiffuse(gray, width, height, FLOYD_STEINBERG, 16)
            DitherMode.ATKINSON -> errorDiffuse(gray, width, height, ATKINSON, 8)
        }
        return pack(black, width, height)
    }

    /** Box-filter downscale (nearest when upscaling) to luminance, compositing alpha over white. */
    private fun scaleToGray(src: ArgbImage, width: Int, height: Int, brightness: Int, invert: Boolean): FloatArray {
        val out = FloatArray(width * height)
        val sx = src.width.toFloat() / width
        val sy = src.height.toFloat() / height
        val offset = brightness.coerceIn(-100, 100) * 2.55f
        for (y in 0 until height) {
            val y0 = (y * sy).toInt()
            val y1 = maxOf(y0 + 1, ((y + 1) * sy).toInt()).coerceAtMost(src.height)
            for (x in 0 until width) {
                val x0 = (x * sx).toInt()
                val x1 = maxOf(x0 + 1, ((x + 1) * sx).toInt()).coerceAtMost(src.width)
                var sum = 0f
                var count = 0
                for (yy in y0 until y1) {
                    val row = yy * src.width
                    for (xx in x0 until x1) {
                        sum += luminance(src.pixels[row + xx])
                        count++
                    }
                }
                var value = (if (count == 0) 255f else sum / count) + offset
                if (invert) value = 255f - value
                out[y * width + x] = value.coerceIn(0f, 255f)
            }
        }
        return out
    }

    private fun luminance(argb: Int): Float {
        val a = (argb ushr 24 and 0xFF) / 255f
        val r = argb shr 16 and 0xFF
        val g = argb shr 8 and 0xFF
        val b = argb and 0xFF
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        return lum * a + 255f * (1f - a)
    }

    private class Kernel(val dx: Int, val dy: Int, val weight: Int)

    private val FLOYD_STEINBERG = listOf(Kernel(1, 0, 7), Kernel(-1, 1, 3), Kernel(0, 1, 5), Kernel(1, 1, 1))
    private val ATKINSON = listOf(
        Kernel(1, 0, 1), Kernel(2, 0, 1), Kernel(-1, 1, 1),
        Kernel(0, 1, 1), Kernel(1, 1, 1), Kernel(0, 2, 1),
    )

    private fun errorDiffuse(gray: FloatArray, width: Int, height: Int, kernel: List<Kernel>, divisor: Int): BooleanArray {
        val black = BooleanArray(gray.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val old = gray[i]
                val isBlack = old < 128f
                black[i] = isBlack
                val err = old - if (isBlack) 0f else 255f
                for (k in kernel) {
                    val nx = x + k.dx
                    val ny = y + k.dy
                    if (nx in 0 until width && ny < height) {
                        gray[ny * width + nx] += err * k.weight / divisor
                    }
                }
            }
        }
        return black
    }

    private fun pack(black: BooleanArray, width: Int, height: Int): MonoBitmap {
        val bytesPerRow = (width + 7) / 8
        val data = ByteArray(bytesPerRow * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (black[y * width + x]) {
                    val idx = y * bytesPerRow + x / 8
                    data[idx] = (data[idx].toInt() or (0x80 ushr (x % 8))).toByte()
                }
            }
        }
        return MonoBitmap(width, height, data)
    }
}
