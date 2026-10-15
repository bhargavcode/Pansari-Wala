package org.bhargav.pansariwala.printer.encoder

import org.bhargav.pansariwala.printer.document.BarcodeType
import org.bhargav.pansariwala.printer.document.Column
import org.bhargav.pansariwala.printer.document.PrintDocument
import org.bhargav.pansariwala.printer.document.PrintElement
import org.bhargav.pansariwala.printer.document.TextStyle
import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintAlignment
import org.bhargav.pansariwala.printer.model.PrintFont
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.printer.text.RasterCell
import org.bhargav.pansariwala.printer.text.RasterFont
import org.bhargav.pansariwala.printer.text.RasterTypeface
import org.bhargav.pansariwala.printer.text.TextRasterizer
import org.bhargav.pansariwala.printer.text.inverted
import org.bhargav.pansariwala.printer.text.needsDeviceFont
import org.bhargav.pansariwala.printer.text.wrapByWidth

/**
 * ESC/POS encoder (Epson-compatible). Works with the vast majority of 58/80 mm thermal printers.
 * With [textRasterizer], text is drawn with the device font (per [PrinterSettings.font]) and sent as
 * raster so any script prints; otherwise non-ASCII is transliterated via [transliterate] since code
 * pages differ per printer.
 */
class EscPosEncoder(
    private val transliterate: (Char) -> String = ::defaultTransliteration,
    private val textRasterizer: TextRasterizer? = null,
) : PrinterCommandEncoder {

    override fun encode(document: PrintDocument, settings: PrinterSettings): ByteArray {
        val out = ByteBuffer(4096)
        val copies = settings.copies.coerceIn(1, 10)
        repeat(copies) { copy ->
            initialize(out, settings)
            document.elements.forEach { encodeElement(out, it, settings) }
            feed(out, settings.feedLinesAfterPrint)
            if (settings.beepAfterPrint && copy == copies - 1) beep(out)
            if (settings.autoCut) cut(out)
        }
        if (settings.openCashDrawer) openDrawer(out)
        return out.toByteArray()
    }

    /** DLE EOT 1: real-time printer status. Harmless on printers that ignore it. */
    override fun statusRequest(): ByteArray = byteArrayOf(DLE, EOT, 1)

    private fun initialize(out: ByteBuffer, settings: PrinterSettings) {
        out.put(ESC.toInt(), '@'.code)
        out.put(ESC.toInt(), 't'.code, 0)
        out.put(ESC.toInt(), 'M'.code, if (settings.fontSize.condensed) 1 else 0)
        if (settings.lineSpacing > 0) {
            out.put(ESC.toInt(), '3'.code, (30 + settings.lineSpacing).coerceIn(0, 255))
        } else {
            out.put(ESC.toInt(), '2'.code)
        }
        // Only sent when changed: a few budget printers echo unknown commands as text.
        if (settings.density != DEFAULT_DENSITY) {
            val density = (settings.density.coerceIn(1, 8) - DEFAULT_DENSITY).let { if (it < 0) 256 + it else it }
            out.put(GS.toInt(), '('.code, 'K'.code, 2, 0, 0x31, density)
        }
        baseSize(out, settings)
    }

    private fun encodeElement(out: ByteBuffer, element: PrintElement, settings: PrinterSettings) {
        when (element) {
            is PrintElement.Text -> {
                val typeface = rasterTypeface(element.text, settings)
                if (typeface != null) {
                    val column = Column(element.text, alignment = element.style.alignment)
                    rasterText(out, listOf(column), element.style, typeface, settings)
                    return
                }
                val cols = columnsFor(element.style, settings)
                applyStyle(out, element.style, settings)
                wrap(element.text, cols).forEach { line ->
                    out.put(encodeText(line)).put(LF.toInt())
                }
                resetStyle(out, settings)
            }

            is PrintElement.Row -> {
                val typeface = rasterTypeface(element.columns.joinToString(" ") { it.text }, settings)
                if (typeface != null) {
                    rasterText(out, element.columns, element.style, typeface, settings)
                    return
                }
                val cols = columnsFor(element.style, settings)
                applyStyle(out, element.style.copy(alignment = PrintAlignment.LEFT), settings)
                layoutRow(element.columns, cols).forEach { line ->
                    out.put(encodeText(line)).put(LF.toInt())
                }
                resetStyle(out, settings)
            }

            is PrintElement.Divider -> {
                out.put(encodeText(element.char.toString().repeat(settings.charsPerLine))).put(LF.toInt())
            }

            is PrintElement.Feed -> feed(out, element.lines)
            is PrintElement.Image -> raster(out, element.bitmap, element.alignment, settings.paperWidth.dots)
            is PrintElement.Barcode -> barcode(out, element)
            is PrintElement.QrCode -> qr(out, element)
            PrintElement.Cut -> cut(out)
            PrintElement.OpenCashDrawer -> openDrawer(out)
            PrintElement.Beep -> beep(out)
        }
    }

    private fun columnsFor(style: TextStyle, settings: PrinterSettings): Int =
        maxOf(8, settings.charsPerLine / style.widthScale.coerceIn(1, 8))

    private fun rasterTypeface(text: String, settings: PrinterSettings): RasterTypeface? {
        if (textRasterizer == null) return null
        return when (settings.font) {
            PrintFont.PRINTER -> null
            PrintFont.AUTO -> RasterTypeface.SANS.takeIf { needsDeviceFont(text) }
            PrintFont.SANS -> RasterTypeface.SANS
            PrintFont.SERIF -> RasterTypeface.SERIF
            PrintFont.MONOSPACE -> RasterTypeface.MONOSPACE
        }
    }

    /** Lays [columns] out on the same character grid as text mode, then draws each line as raster. */
    private fun rasterText(
        out: ByteBuffer,
        columns: List<Column>,
        style: TextStyle,
        typeface: RasterTypeface,
        settings: PrinterSettings,
    ) {
        val rasterizer = textRasterizer ?: return
        val paperDots = settings.paperWidth.dots
        val cols = columnsFor(style, settings)
        val dotsPerChar = paperDots.toFloat() / cols
        val scale = maxOf(
            settings.fontSize.widthScale * style.widthScale,
            settings.fontSize.heightScale * style.heightScale,
        ).coerceIn(1, 8)
        val basePx = if (settings.fontSize.condensed) RASTER_TEXT_PX_CONDENSED else RASTER_TEXT_PX
        val font = RasterFont(typeface, basePx * scale, style.bold, style.underline)

        val widths = columnWidths(columns, cols)
        var cursor = 0f
        val spans = widths.map { chars ->
            val left = cursor.toInt()
            val width = (chars * dotsPerChar).toInt()
            cursor += (chars + 1) * dotsPerChar
            left to width
        }
        val cells = columns.mapIndexed { i, col ->
            wrapByWidth(col.text, spans[i].second.toFloat()) { rasterizer.measure(it, font) }
        }
        val height = cells.maxOf { it.size }
        for (line in 0 until height) {
            val lineCells = columns.mapIndexedNotNull { i, col ->
                cells[i].getOrNull(line)?.takeIf { it.isNotEmpty() }
                    ?.let { RasterCell(it, spans[i].first, spans[i].second, col.alignment) }
            }
            val bitmap = rasterizer.renderLine(lineCells, paperDots, font)
            raster(out, if (style.inverted) bitmap.inverted() else bitmap, PrintAlignment.LEFT, paperDots)
            if (settings.lineSpacing > 0) out.put(ESC.toInt(), 'J'.code, settings.lineSpacing.coerceIn(0, 255))
        }
    }

    private fun applyStyle(out: ByteBuffer, style: TextStyle, settings: PrinterSettings) {
        out.put(ESC.toInt(), 'a'.code, style.alignment.ordinal)
        out.put(ESC.toInt(), 'E'.code, if (style.bold) 1 else 0)
        out.put(ESC.toInt(), '-'.code, if (style.underline) 1 else 0)
        out.put(GS.toInt(), 'B'.code, if (style.inverted) 1 else 0)
        val w = (settings.fontSize.widthScale * style.widthScale).coerceIn(1, 8)
        val h = (settings.fontSize.heightScale * style.heightScale).coerceIn(1, 8)
        out.put(GS.toInt(), '!'.code, ((w - 1) shl 4) or (h - 1))
    }

    private fun resetStyle(out: ByteBuffer, settings: PrinterSettings) {
        out.put(ESC.toInt(), 'a'.code, 0)
        out.put(ESC.toInt(), 'E'.code, 0)
        out.put(ESC.toInt(), '-'.code, 0)
        out.put(GS.toInt(), 'B'.code, 0)
        baseSize(out, settings)
    }

    private fun baseSize(out: ByteBuffer, settings: PrinterSettings) {
        val w = settings.fontSize.widthScale
        val h = settings.fontSize.heightScale
        out.put(GS.toInt(), '!'.code, ((w - 1) shl 4) or (h - 1))
    }

    private fun feed(out: ByteBuffer, lines: Int) {
        if (lines > 0) out.put(ESC.toInt(), 'd'.code, lines.coerceAtMost(255))
    }

    private fun cut(out: ByteBuffer) {
        out.put(GS.toInt(), 'V'.code, 66, 0)
    }

    private fun openDrawer(out: ByteBuffer) {
        out.put(ESC.toInt(), 'p'.code, 0, 25, 250)
    }

    private fun beep(out: ByteBuffer) {
        out.put(ESC.toInt(), 'B'.code, 3, 2)
    }

    /** GS v 0 raster in bands so small printer buffers never overflow. Alignment is baked into the raster. */
    private fun raster(out: ByteBuffer, bitmap: MonoBitmap, alignment: PrintAlignment, paperDots: Int) {
        val targetWidth = maxOf(bitmap.width, paperDots)
        val offset = when (alignment) {
            PrintAlignment.LEFT -> 0
            PrintAlignment.CENTER -> (targetWidth - bitmap.width).coerceAtLeast(0) / 2
            PrintAlignment.RIGHT -> (targetWidth - bitmap.width).coerceAtLeast(0)
        }
        val bytesPerRow = (targetWidth + 7) / 8
        out.put(ESC.toInt(), 'a'.code, 0)
        var y = 0
        while (y < bitmap.height) {
            val band = minOf(RASTER_BAND_ROWS, bitmap.height - y)
            out.put(GS.toInt(), 'v'.code, '0'.code, 0, bytesPerRow and 0xFF, bytesPerRow shr 8, band and 0xFF, band shr 8)
            val row = ByteArray(bytesPerRow)
            for (by in 0 until band) {
                row.fill(0)
                val sy = y + by
                for (x in 0 until bitmap.width) {
                    val tx = x + offset
                    if (tx < targetWidth && bitmap.isBlack(x, sy)) {
                        row[tx / 8] = (row[tx / 8].toInt() or (0x80 ushr (tx % 8))).toByte()
                    }
                }
                out.put(row)
            }
            y += band
        }
    }

    private fun barcode(out: ByteBuffer, element: PrintElement.Barcode) {
        val data = encodeText(element.data)
        out.put(ESC.toInt(), 'a'.code, element.alignment.ordinal)
        out.put(GS.toInt(), 'h'.code, element.height.coerceIn(1, 255))
        out.put(GS.toInt(), 'w'.code, 2)
        out.put(GS.toInt(), 'H'.code, if (element.showText) 2 else 0)
        when (element.type) {
            BarcodeType.CODE128 -> {
                val payload = byteArrayOf('{'.code.toByte(), 'B'.code.toByte()) + data
                out.put(GS.toInt(), 'k'.code, 73, payload.size).put(payload)
            }
            BarcodeType.EAN13 -> out.put(GS.toInt(), 'k'.code, 67, data.size).put(data)
            BarcodeType.EAN8 -> out.put(GS.toInt(), 'k'.code, 68, data.size).put(data)
            BarcodeType.UPC_A -> out.put(GS.toInt(), 'k'.code, 65, data.size).put(data)
            BarcodeType.CODE39 -> out.put(GS.toInt(), 'k'.code, 69, data.size).put(data)
        }
        out.put(LF.toInt())
        out.put(ESC.toInt(), 'a'.code, 0)
    }

    private fun qr(out: ByteBuffer, element: PrintElement.QrCode) {
        val data = encodeText(element.data)
        val len = data.size + 3
        out.put(ESC.toInt(), 'a'.code, element.alignment.ordinal)
        out.put(GS.toInt(), '('.code, 'k'.code, 4, 0, 0x31, 0x41, 0x32, 0)
        out.put(GS.toInt(), '('.code, 'k'.code, 3, 0, 0x31, 0x43, element.moduleSize.coerceIn(1, 16))
        out.put(GS.toInt(), '('.code, 'k'.code, 3, 0, 0x31, 0x45, 0x31)
        out.put(GS.toInt(), '('.code, 'k'.code, len and 0xFF, len shr 8, 0x31, 0x50, 0x30).put(data)
        out.put(GS.toInt(), '('.code, 'k'.code, 3, 0, 0x31, 0x51, 0x30)
        out.put(LF.toInt())
        out.put(ESC.toInt(), 'a'.code, 0)
    }

    private fun encodeText(text: String): ByteArray {
        val sb = StringBuilder(text.length)
        text.forEach { c -> if (c.code in 0x20..0x7E || c == '\n') sb.append(c) else sb.append(transliterate(c)) }
        return sb.toString().encodeToByteArray()
    }

    companion object {
        private const val ESC: Byte = 0x1B
        private const val GS: Byte = 0x1D
        private const val DLE: Byte = 0x10
        private const val EOT: Byte = 0x04
        private const val LF: Byte = 0x0A
        private const val RASTER_BAND_ROWS = 48
        private const val DEFAULT_DENSITY = 4
        private const val RASTER_TEXT_PX = 22f
        private const val RASTER_TEXT_PX_CONDENSED = 18f

        fun defaultTransliteration(c: Char): String = when (c) {
            '\u20B9' -> "Rs."
            '\u2013', '\u2014' -> "-"
            '\u2018', '\u2019' -> "'"
            '\u201C', '\u201D' -> "\""
            '\u2022' -> "*"
            '\u00D7' -> "x"
            '\t' -> " "
            else -> "?"
        }

        /** Word-wraps [text] to [width] characters, hard-breaking words that are longer than a line. */
        fun wrap(text: String, width: Int): List<String> {
            if (width <= 0) return listOf(text)
            val lines = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                var current = StringBuilder()
                paragraph.split(' ').forEach { word ->
                    var w = word
                    while (w.length > width) {
                        if (current.isNotEmpty()) {
                            lines += current.toString()
                            current = StringBuilder()
                        }
                        lines += w.take(width)
                        w = w.drop(width)
                    }
                    when {
                        current.isEmpty() -> current.append(w)
                        current.length + 1 + w.length <= width -> current.append(' ').append(w)
                        else -> {
                            lines += current.toString()
                            current = StringBuilder(w)
                        }
                    }
                }
                lines += current.toString()
            }
            return lines
        }

        /** Lays out columns into fixed-width text lines; cells wrap and the row grows to the tallest cell. */
        fun layoutRow(columns: List<Column>, width: Int): List<String> {
            if (columns.isEmpty()) return emptyList()
            val widths = columnWidths(columns, width)
            val cells = columns.mapIndexed { i, col -> wrap(col.text, widths[i]) }
            val height = cells.maxOf { it.size }
            return (0 until height).map { line ->
                buildString {
                    columns.forEachIndexed { i, col ->
                        if (i > 0) append(' ')
                        val text = cells[i].getOrElse(line) { "" }
                        val pad = (widths[i] - text.length).coerceAtLeast(0)
                        when (col.alignment) {
                            PrintAlignment.LEFT -> append(text).append(" ".repeat(pad))
                            PrintAlignment.RIGHT -> append(" ".repeat(pad)).append(text)
                            PrintAlignment.CENTER -> append(" ".repeat(pad / 2)).append(text).append(" ".repeat(pad - pad / 2))
                        }
                    }
                }.trimEnd()
            }
        }

        /** Character width of each column, leaving one space between neighbours. */
        fun columnWidths(columns: List<Column>, width: Int): IntArray {
            val gaps = columns.size - 1
            val fixed = columns.sumOf { it.fixedChars.coerceAtLeast(0) }
            val flexible = columns.filter { it.fixedChars <= 0 }
            val remaining = (width - fixed - gaps).coerceAtLeast(flexible.size)
            val totalWeight = flexible.sumOf { it.weight.toDouble() }.takeIf { it > 0 } ?: 1.0
            val widths = IntArray(columns.size)
            var assigned = 0
            var lastFlexible = -1
            columns.forEachIndexed { i, col ->
                if (col.fixedChars > 0) {
                    widths[i] = col.fixedChars
                } else {
                    widths[i] = maxOf(1, (remaining * col.weight / totalWeight).toInt())
                    assigned += widths[i]
                    lastFlexible = i
                }
            }
            if (lastFlexible >= 0) widths[lastFlexible] += remaining - assigned
            return widths
        }
    }
}
