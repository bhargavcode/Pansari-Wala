package org.bhargav.pansariwala.printer

import org.bhargav.pansariwala.printer.document.Column
import org.bhargav.pansariwala.printer.document.PrintLabels
import org.bhargav.pansariwala.printer.document.PrintLineItem
import org.bhargav.pansariwala.printer.document.PrintTemplates
import org.bhargav.pansariwala.printer.document.ReceiptData
import org.bhargav.pansariwala.printer.document.printDocument
import org.bhargav.pansariwala.printer.encoder.EscPosEncoder
import org.bhargav.pansariwala.printer.image.ImageProcessor
import org.bhargav.pansariwala.printer.model.ArgbImage
import org.bhargav.pansariwala.printer.model.DitherMode
import org.bhargav.pansariwala.printer.model.PrintAlignment
import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintFont
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.printer.text.RasterCell
import org.bhargav.pansariwala.printer.text.RasterFont
import org.bhargav.pansariwala.printer.text.TextRasterizer
import org.bhargav.pansariwala.printer.text.monoBitmapOf
import org.bhargav.pansariwala.printer.text.wrapByWidth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrinterCoreTest {

    @Test
    fun wrapBreaksOnWordsAndHardSplitsLongWords() {
        assertEquals(listOf("hello", "world"), EscPosEncoder.wrap("hello world", 6))
        assertEquals(listOf("abcd", "ef"), EscPosEncoder.wrap("abcdef", 4))
    }

    @Test
    fun rowLayoutFillsExactWidthWithAlignment() {
        val lines = EscPosEncoder.layoutRow(
            listOf(Column("Sugar", weight = 1f), Column("45.00", fixedChars = 8, alignment = PrintAlignment.RIGHT)),
            width = 20,
        )
        assertEquals(listOf("Sugar          45.00"), lines)
    }

    @Test
    fun amountFormatsTwoDecimals() {
        assertEquals("12.50", PrintTemplates.amount(12.5))
        assertEquals("-0.10", PrintTemplates.amount(-0.1))
        assertEquals("0.00", PrintTemplates.amount(0.0))
    }

    @Test
    fun encoderStartsWithInitAndEndsWithCut() {
        val bytes = EscPosEncoder().encode(printDocument { text("Hi") }, PrinterSettings(autoCut = true, feedLinesAfterPrint = 0))
        assertEquals(0x1B, bytes[0].toInt())
        assertEquals('@'.code, bytes[1].toInt())
        val tail = bytes.takeLast(4).map { it.toInt() }
        assertEquals(listOf(0x1D, 'V'.code, 66, 0), tail)
    }

    @Test
    fun copiesRepeatTheJob() {
        val one = EscPosEncoder().encode(printDocument { text("X") }, PrinterSettings(copies = 1))
        val two = EscPosEncoder().encode(printDocument { text("X") }, PrinterSettings(copies = 2))
        assertEquals(one.size * 2, two.size)
    }

    @Test
    fun nonAsciiIsTransliterated() {
        val bytes = EscPosEncoder().encode(printDocument { text("\u20B95") }, PrinterSettings(autoCut = false, feedLinesAfterPrint = 0))
        assertTrue(bytes.decodeToString().contains("Rs.5"))
    }

    private class FakeRasterizer : TextRasterizer {
        val rendered = mutableListOf<List<RasterCell>>()
        override fun measure(text: String, font: RasterFont): Float = text.length * 10f
        override fun renderLine(cells: List<RasterCell>, widthPx: Int, font: RasterFont): MonoBitmap {
            rendered += cells
            return monoBitmapOf(widthPx, 4) { _, _ -> false }
        }
    }

    @Test
    fun autoFontRastersOnlyIndianText() {
        val rasterizer = FakeRasterizer()
        val encoder = EscPosEncoder(textRasterizer = rasterizer)
        val settings = PrinterSettings(autoCut = false, feedLinesAfterPrint = 0)
        encoder.encode(printDocument { text("Sugar") }, settings)
        assertTrue(rasterizer.rendered.isEmpty())
        encoder.encode(printDocument { keyValue("चीनी", "45.00") }, settings)
        assertEquals(listOf("चीनी", "45.00"), rasterizer.rendered.single().map { it.text })
    }

    @Test
    fun printerFontNeverRasters() {
        val rasterizer = FakeRasterizer()
        EscPosEncoder(textRasterizer = rasterizer)
            .encode(printDocument { text("चीनी") }, PrinterSettings(font = PrintFont.PRINTER))
        assertTrue(rasterizer.rendered.isEmpty())
    }

    @Test
    fun widthWrapKeepsCombiningMarksWithBaseLetter() {
        val lines = wrapByWidth("चीनी", 20f) { it.length * 10f }
        assertEquals(listOf("ची", "नी"), lines)
    }

    @Test
    fun monochromeConversionKeepsAspectAndColours() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val image = ArgbImage(4, 2, intArrayOf(black, black, white, white, black, black, white, white))
        val mono = ImageProcessor.toMonochrome(image, targetWidth = 8, mode = DitherMode.THRESHOLD)
        assertEquals(8, mono.width)
        assertEquals(4, mono.height)
        assertTrue(mono.isBlack(0, 0))
        assertTrue(!mono.isBlack(7, 0))
    }

    @Test
    fun gammaLightensMidGreyForThermalDotGain() {
        val grey = ArgbImage(16, 16, IntArray(256) { 0xFF808080.toInt() })
        fun blackDots(mode: DitherMode, gamma: Int): Int {
            val mono = ImageProcessor.toMonochrome(grey, 16, mode = mode, gammaPercent = gamma)
            return (0 until 16).sumOf { y -> (0 until 16).count { x -> mono.isBlack(x, y) } }
        }
        for (mode in listOf(DitherMode.FLOYD_STEINBERG, DitherMode.ORDERED)) {
            val neutral = blackDots(mode, 100)
            assertTrue(neutral in 1 until 256, "$mode should dither mid-grey, not fill it")
            assertTrue(blackDots(mode, 180) < neutral, "$mode gamma should lighten mid-grey")
        }
    }

    @Test
    fun settingsRoundTripThroughCodec() {
        val settings = PrinterSettings(
            lastDevice = networkPrinterDevice("192.168.1.50"),
            copies = 2,
            image = PrinterSettings().image.copy(
                logo = ImageProcessor.toMonochrome(ArgbImage(1, 1, intArrayOf(0xFF000000.toInt())), 8),
            ),
        )
        assertEquals(settings, PrinterSettingsCodec.decode(PrinterSettingsCodec.encode(settings)))
    }

    @Test
    fun receiptTemplateContainsTotals() {
        val doc = PrintTemplates.receipt(
            ReceiptData(
                orderNumber = "A1",
                dateTime = "01/01/2026 10:00",
                items = listOf(PrintLineItem("Sugar", "1 kg", 45.0, 45.0)),
                total = 45.0,
            ),
            PrinterSettings(),
            labels,
        )
        val text = EscPosEncoder().encode(doc, PrinterSettings()).decodeToString()
        assertTrue(text.contains("Sugar"))
        assertTrue(text.contains("45.00"))
    }

    private val labels = PrintLabels(
        receiptTitle = "R", invoiceTitle = "I", orderTicketTitle = "O", inventoryTitle = "Inv", lowStockTitle = "L",
        testPrintTitle = "T", orderNumber = "No", invoiceNumber = "No", date = "D", customer = "C", cashier = "Ca",
        item = "Item", quantity = "Q", rate = "R", amount = "A", subtotal = "S", discount = "Di", tax = "Tx",
        total = "TOTAL", paid = "P", change = "Ch", paymentMode = "Pm", billTo = "B", phone = "Ph", taxId = "G",
        address = "Ad", note = "N", channel = "Chn", status = "St", category = "Cat", stock = "Stk", value = "V",
        lowStockMark = "*", totalItems = "Ti", totalValue = "Tv", price = "Pr",
    )
}
