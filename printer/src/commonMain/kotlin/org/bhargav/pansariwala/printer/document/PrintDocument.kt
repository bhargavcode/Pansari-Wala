package org.bhargav.pansariwala.printer.document

import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintAlignment

/**
 * Printer-agnostic description of what to print. Encoders (ESC/POS today; TSPL/ZPL/StarPRNT later)
 * turn this into bytes, so templates never depend on a printer command language.
 */
data class PrintDocument(val elements: List<PrintElement>)

data class TextStyle(
    val alignment: PrintAlignment = PrintAlignment.LEFT,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val inverted: Boolean = false,
    /** Multiplier applied on top of the configured base font size (1..4). */
    val widthScale: Int = 1,
    val heightScale: Int = 1,
)

enum class BarcodeType { CODE128, EAN13, EAN8, UPC_A, CODE39 }

data class Column(
    val text: String,
    /** Relative width; ignored when [fixedChars] > 0. */
    val weight: Float = 1f,
    val fixedChars: Int = 0,
    val alignment: PrintAlignment = PrintAlignment.LEFT,
)

sealed interface PrintElement {
    data class Text(val text: String, val style: TextStyle = TextStyle()) : PrintElement
    data class Row(val columns: List<Column>, val style: TextStyle = TextStyle()) : PrintElement
    data class Divider(val char: Char = '-') : PrintElement
    data class Feed(val lines: Int = 1) : PrintElement
    data class Image(val bitmap: MonoBitmap, val alignment: PrintAlignment = PrintAlignment.CENTER) : PrintElement
    data class Barcode(
        val data: String,
        val type: BarcodeType = BarcodeType.CODE128,
        val height: Int = 80,
        val showText: Boolean = true,
        val alignment: PrintAlignment = PrintAlignment.CENTER,
    ) : PrintElement
    data class QrCode(val data: String, val moduleSize: Int = 6, val alignment: PrintAlignment = PrintAlignment.CENTER) : PrintElement
    data object Cut : PrintElement
    data object OpenCashDrawer : PrintElement
    data object Beep : PrintElement
}

@DslMarker
annotation class PrintDsl

@PrintDsl
class PrintDocumentBuilder {
    private val elements = mutableListOf<PrintElement>()

    fun text(value: String, style: TextStyle = TextStyle()) {
        elements += PrintElement.Text(value, style)
    }

    fun title(value: String, alignment: PrintAlignment = PrintAlignment.CENTER, scale: Int = 2) =
        text(value, TextStyle(alignment = alignment, bold = true, widthScale = scale, heightScale = scale))

    fun center(value: String, bold: Boolean = false) =
        text(value, TextStyle(alignment = PrintAlignment.CENTER, bold = bold))

    fun row(vararg columns: Column, style: TextStyle = TextStyle()) {
        elements += PrintElement.Row(columns.toList(), style)
    }

    /** Label on the left, value pushed to the right edge. */
    fun keyValue(key: String, value: String, bold: Boolean = false) =
        row(Column(key, weight = 1f), Column(value, weight = 1f, alignment = PrintAlignment.RIGHT), style = TextStyle(bold = bold))

    fun divider(char: Char = '-') {
        elements += PrintElement.Divider(char)
    }

    fun feed(lines: Int = 1) {
        if (lines > 0) elements += PrintElement.Feed(lines)
    }

    fun image(bitmap: MonoBitmap, alignment: PrintAlignment = PrintAlignment.CENTER) {
        elements += PrintElement.Image(bitmap, alignment)
    }

    fun barcode(data: String, type: BarcodeType = BarcodeType.CODE128, height: Int = 80) {
        if (data.isNotBlank()) elements += PrintElement.Barcode(data, type, height)
    }

    fun qr(data: String, moduleSize: Int = 6) {
        if (data.isNotBlank()) elements += PrintElement.QrCode(data, moduleSize)
    }

    fun cut() {
        elements += PrintElement.Cut
    }

    fun openCashDrawer() {
        elements += PrintElement.OpenCashDrawer
    }

    fun beep() {
        elements += PrintElement.Beep
    }

    fun include(document: PrintDocument) {
        elements += document.elements
    }

    fun build(): PrintDocument = PrintDocument(elements.toList())
}

fun printDocument(block: PrintDocumentBuilder.() -> Unit): PrintDocument =
    PrintDocumentBuilder().apply(block).build()
