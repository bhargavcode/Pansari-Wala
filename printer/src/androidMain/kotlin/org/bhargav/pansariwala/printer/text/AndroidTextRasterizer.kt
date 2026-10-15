package org.bhargav.pansariwala.printer.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintAlignment

class AndroidTextRasterizer : TextRasterizer {

    override fun measure(text: String, font: RasterFont): Float = paint(font).measureText(text)

    override fun renderLine(cells: List<RasterCell>, widthPx: Int, font: RasterFont): MonoBitmap {
        val paint = paint(font)
        val metrics = paint.fontMetricsInt
        val height = (metrics.bottom - metrics.top + LINE_PADDING_PX).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val baseline = (-metrics.top + LINE_PADDING_PX / 2).toFloat()
            cells.forEach { cell ->
                val textWidth = paint.measureText(cell.text)
                val x = when (cell.alignment) {
                    PrintAlignment.LEFT -> cell.left.toFloat()
                    PrintAlignment.CENTER -> cell.left + (cell.width - textWidth) / 2f
                    PrintAlignment.RIGHT -> cell.left + cell.width - textWidth
                }
                canvas.drawText(cell.text, x.coerceAtLeast(0f), baseline, paint)
            }
            val pixels = IntArray(widthPx * height)
            bitmap.getPixels(pixels, 0, widthPx, 0, 0, widthPx, height)
            return monoBitmapOf(widthPx, height) { x, y -> isDark(pixels[y * widthPx + x]) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun paint(font: RasterFont) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val family = when (font.typeface) {
            RasterTypeface.SANS -> Typeface.SANS_SERIF
            RasterTypeface.SERIF -> Typeface.SERIF
            RasterTypeface.MONOSPACE -> Typeface.MONOSPACE
        }
        typeface = Typeface.create(family, if (font.bold) Typeface.BOLD else Typeface.NORMAL)
        textSize = font.sizePx
        color = Color.BLACK
        isUnderlineText = font.underline
    }

    private fun isDark(argb: Int): Boolean {
        val luminance = (Color.red(argb) * 299 + Color.green(argb) * 587 + Color.blue(argb) * 114) / 1000
        return luminance < DARK_THRESHOLD
    }

    private companion object {
        const val LINE_PADDING_PX = 4
        const val DARK_THRESHOLD = 140
    }
}
