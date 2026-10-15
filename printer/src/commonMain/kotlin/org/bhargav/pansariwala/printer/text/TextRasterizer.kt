package org.bhargav.pansariwala.printer.text

import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintAlignment

enum class RasterTypeface { SANS, SERIF, MONOSPACE }

data class RasterFont(
    val typeface: RasterTypeface,
    val sizePx: Float,
    val bold: Boolean = false,
    val underline: Boolean = false,
)

/** One piece of text placed inside the horizontal span [left, left + width) of a line. */
data class RasterCell(
    val text: String,
    val left: Int,
    val width: Int,
    val alignment: PrintAlignment = PrintAlignment.LEFT,
)

/**
 * Draws text with the platform font engine (complex-script shaping + system font fallback), so
 * Devanagari, Bengali, Gurmukhi, Gujarati, Odia, Tamil, Telugu, Kannada, Malayalam etc. print
 * correctly on printers whose built-in fonts are ASCII-only.
 */
interface TextRasterizer {
    fun measure(text: String, font: RasterFont): Float

    /** Renders one line [widthPx] dots wide; the platform picks a height that fits every glyph. */
    fun renderLine(cells: List<RasterCell>, widthPx: Int, font: RasterFont): MonoBitmap
}

/** Packs a 1-bit raster from a per-pixel predicate. */
fun monoBitmapOf(width: Int, height: Int, isBlack: (x: Int, y: Int) -> Boolean): MonoBitmap {
    val bytesPerRow = (width + 7) / 8
    val data = ByteArray(bytesPerRow * height)
    for (y in 0 until height) {
        val row = y * bytesPerRow
        for (x in 0 until width) {
            if (isBlack(x, y)) data[row + x / 8] = (data[row + x / 8].toInt() or (0x80 ushr (x % 8))).toByte()
        }
    }
    return MonoBitmap(width, height, data)
}

fun MonoBitmap.inverted(): MonoBitmap = monoBitmapOf(width, height) { x, y -> !isBlack(x, y) }

/** True for non-ASCII letters/marks (any Indian script) that an ASCII printer font cannot show. */
internal fun needsDeviceFont(text: String): Boolean =
    text.any { c -> c.code > 0x7E && (c.isLetterOrDigit() || isClusterContinuation(c)) }

/**
 * Word-wraps [text] to [maxPx] using measured widths. Over-long words are broken between
 * characters but never before a combining mark, so Indic clusters stay intact.
 */
internal fun wrapByWidth(text: String, maxPx: Float, measure: (String) -> Float): List<String> {
    val lines = mutableListOf<String>()
    text.split('\n').forEach { paragraph ->
        var current = ""
        paragraph.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            when {
                measure(candidate) <= maxPx -> current = candidate
                measure(word) <= maxPx -> {
                    lines += current
                    current = word
                }
                else -> {
                    if (current.isNotEmpty()) lines += current
                    current = ""
                    var piece = ""
                    var i = 0
                    while (i < word.length) {
                        var end = i + 1
                        while (end < word.length && isClusterContinuation(word[end])) end++
                        val chunk = word.substring(i, end)
                        if (piece.isNotEmpty() && measure(piece + chunk) > maxPx) {
                            lines += piece
                            piece = chunk
                        } else {
                            piece += chunk
                        }
                        i = end
                    }
                    current = piece
                }
            }
        }
        lines += current
    }
    return lines
}

private fun isClusterContinuation(c: Char): Boolean =
    c.category == CharCategory.NON_SPACING_MARK ||
        c.category == CharCategory.COMBINING_SPACING_MARK ||
        c.category == CharCategory.ENCLOSING_MARK ||
        c.category == CharCategory.FORMAT ||
        c.isLowSurrogate()
