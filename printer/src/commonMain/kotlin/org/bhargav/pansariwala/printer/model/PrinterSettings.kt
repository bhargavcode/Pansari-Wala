package org.bhargav.pansariwala.printer.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaperWidth(val millimeters: Int, val dots: Int, val charsPerLine: Int) {
    MM_58(58, 384, 32),
    MM_80(80, 576, 48),
}

/** Base font scale. Maps to ESC/POS character size multipliers. */
@Serializable
enum class PrintFontSize(val widthScale: Int, val heightScale: Int, val condensed: Boolean) {
    SMALL(1, 1, true),
    NORMAL(1, 1, false),
    LARGE(1, 2, false),
    EXTRA_LARGE(2, 2, false),
}

/**
 * Typeface for receipt text. Printer built-in fonts only cover ASCII, so every other option is
 * drawn with the device font engine and sent as an image, which prints all Indian scripts.
 */
@Serializable
enum class PrintFont {
    /** Built-in font for English lines; device font for lines with Indian-language text. */
    AUTO,
    /** Built-in printer font only. Fastest, but non-English text is replaced. */
    PRINTER,
    SANS,
    SERIF,
    MONOSPACE,
}

@Serializable
enum class PrintAlignment { LEFT, CENTER, RIGHT }

@Serializable
enum class DitherMode { THRESHOLD, FLOYD_STEINBERG, ATKINSON }

@Serializable
data class PrintHeaderFooter(
    val shopName: String = "",
    val addressLines: String = "",
    val phone: String = "",
    val taxId: String = "",
    val headerNote: String = "",
    val footerNote: String = "",
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val headerAlignment: PrintAlignment = PrintAlignment.CENTER,
    val boldShopName: Boolean = true,
)

@Serializable
data class PrintImageSettings(
    val printLogo: Boolean = false,
    /** PNG/JPEG already converted to a 1-bit raster; stored so printing needs no decoding. */
    val logo: MonoBitmap? = null,
    /** Logo width as a percentage of the printable width (10..100). */
    val logoWidthPercent: Int = 60,
    val ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
    /** 0..255. Pixels darker than this become black when using [DitherMode.THRESHOLD]. */
    val threshold: Int = 128,
    /** -100..100 brightness offset applied before dithering. */
    val brightness: Int = 0,
    val invert: Boolean = false,
)

@Serializable
data class PrinterSettings(
    val lastDevice: PrinterDevice? = null,
    val autoReconnect: Boolean = true,
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    /** Overrides [PaperWidth.charsPerLine] when > 0 (some printers use 42 / 46 / 64 columns). */
    val customCharsPerLine: Int = 0,
    val fontSize: PrintFontSize = PrintFontSize.NORMAL,
    val font: PrintFont = PrintFont.AUTO,
    val boldTitles: Boolean = true,
    /** Extra dots between lines (0..60); 0 keeps printer default. */
    val lineSpacing: Int = 0,
    /** Darkness 1..8 for printers that understand GS ( K; ignored otherwise. */
    val density: Int = 4,
    val headerFooter: PrintHeaderFooter = PrintHeaderFooter(),
    val image: PrintImageSettings = PrintImageSettings(),
    val showQrOnReceipt: Boolean = false,
    val qrContent: String = "",
    val showBarcodeOnReceipt: Boolean = false,
    val feedLinesAfterPrint: Int = 3,
    val autoCut: Boolean = true,
    val openCashDrawer: Boolean = false,
    val beepAfterPrint: Boolean = false,
    val copies: Int = 1,
    val autoPrintReceipt: Boolean = false,
    val currencySymbol: String = "Rs.",
) {
    val charsPerLine: Int
        get() {
            val base = if (customCharsPerLine > 0) customCharsPerLine else paperWidth.charsPerLine
            return if (fontSize.widthScale > 1) base / fontSize.widthScale else if (fontSize.condensed) base * 4 / 3 else base
        }
}
