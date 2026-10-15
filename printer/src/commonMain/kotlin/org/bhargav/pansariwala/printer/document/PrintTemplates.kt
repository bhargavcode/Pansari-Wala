package org.bhargav.pansariwala.printer.document

import org.bhargav.pansariwala.printer.model.PrintAlignment
import org.bhargav.pansariwala.printer.model.PrinterSettings
import kotlin.math.abs
import kotlin.math.roundToLong

/** Ready-made layouts. Each returns a [PrintDocument]; output options (cut, drawer, copies) are applied by the encoder. */
object PrintTemplates {

    private const val WIDE_LAYOUT_MIN_CHARS = 42

    fun receipt(data: ReceiptData, settings: PrinterSettings, labels: PrintLabels): PrintDocument = printDocument {
        header(settings, labels)
        title(labels.receiptTitle, scale = 1)
        divider()
        keyValue(labels.orderNumber, data.orderNumber)
        keyValue(labels.date, data.dateTime)
        data.customerName?.takeIf { it.isNotBlank() }?.let { keyValue(labels.customer, it) }
        data.cashier?.takeIf { it.isNotBlank() }?.let { keyValue(labels.cashier, it) }
        divider()
        items(data.items, settings, labels)
        divider()
        val currency = settings.currencySymbol
        keyValue(labels.subtotal, money(data.subtotal, currency))
        if (data.discount != 0.0) keyValue(labels.discount, "-" + money(data.discount, currency))
        if (data.tax != 0.0) keyValue(labels.tax, money(data.tax, currency))
        divider('=')
        text(
            padBetween(labels.total, money(data.total, currency), settings.charsPerLine / 2),
            TextStyle(bold = true, widthScale = 2, heightScale = 2),
        )
        divider('=')
        data.paymentMode?.takeIf { it.isNotBlank() }?.let { keyValue(labels.paymentMode, it) }
        data.paid?.let { keyValue(labels.paid, money(it, currency)) }
        data.change?.let { keyValue(labels.change, money(it, currency)) }
        data.note?.takeIf { it.isNotBlank() }?.let { text("${labels.note}: $it") }
        if (settings.showBarcodeOnReceipt) {
            feed()
            barcode(data.orderNumber)
        }
        if (settings.showQrOnReceipt) {
            feed()
            qr(settings.qrContent.ifBlank { data.orderNumber })
        }
        footer(settings)
    }

    fun orderTicket(data: OrderTicketData, settings: PrinterSettings, labels: PrintLabels): PrintDocument = printDocument {
        header(settings, labels, compact = true)
        title(labels.orderTicketTitle)
        title("#${data.orderNumber}", scale = 2)
        divider()
        keyValue(labels.date, data.dateTime)
        data.customerName?.takeIf { it.isNotBlank() }?.let { keyValue(labels.customer, it) }
        data.channel?.takeIf { it.isNotBlank() }?.let { keyValue(labels.channel, it) }
        data.status?.takeIf { it.isNotBlank() }?.let { keyValue(labels.status, it) }
        divider()
        row(
            Column(labels.item, weight = 1f),
            Column(labels.quantity, fixedChars = 10, alignment = PrintAlignment.RIGHT),
            style = TextStyle(bold = true),
        )
        data.items.forEach { item ->
            row(
                Column(item.name, weight = 1f),
                Column(item.quantity, fixedChars = 10, alignment = PrintAlignment.RIGHT),
                style = TextStyle(heightScale = 2),
            )
            item.secondaryText?.takeIf { it.isNotBlank() }?.let { text("  $it") }
        }
        divider()
        keyValue(labels.totalItems, data.items.size.toString(), bold = true)
        data.deliveryAddress?.takeIf { it.isNotBlank() }?.let {
            divider()
            text("${labels.address}:", TextStyle(bold = true))
            text(it)
        }
        data.note?.takeIf { it.isNotBlank() }?.let {
            text("${labels.note}:", TextStyle(bold = true))
            text(it)
        }
        feed()
    }

    fun invoice(data: InvoiceData, settings: PrinterSettings, labels: PrintLabels): PrintDocument = printDocument {
        header(settings, labels)
        title(labels.invoiceTitle, scale = 1)
        divider()
        keyValue(labels.invoiceNumber, data.invoiceNumber)
        keyValue(labels.date, data.dateTime)
        if (!data.billToName.isNullOrBlank() || !data.billToAddress.isNullOrBlank()) {
            divider()
            text("${labels.billTo}:", TextStyle(bold = true))
            data.billToName?.takeIf { it.isNotBlank() }?.let { text(it) }
            data.billToAddress?.takeIf { it.isNotBlank() }?.let { text(it) }
            data.billToPhone?.takeIf { it.isNotBlank() }?.let { text("${labels.phone}: $it") }
        }
        divider()
        items(data.items, settings, labels, showTax = true)
        divider()
        val currency = settings.currencySymbol
        keyValue(labels.subtotal, money(data.subtotal, currency))
        if (data.discount != 0.0) keyValue(labels.discount, "-" + money(data.discount, currency))
        keyValue(labels.tax, money(data.taxTotal, currency))
        divider('=')
        keyValue(labels.total, money(data.total, currency), bold = true)
        divider('=')
        data.paymentMode?.takeIf { it.isNotBlank() }?.let { keyValue(labels.paymentMode, it) }
        data.terms?.takeIf { it.isNotBlank() }?.let {
            feed()
            text(it)
        }
        if (settings.showQrOnReceipt) {
            feed()
            qr(settings.qrContent.ifBlank { data.invoiceNumber })
        }
        footer(settings)
    }

    /** Shelf / product labels. One document can hold several labels separated by cuts. */
    fun labels(data: List<LabelData>, settings: PrinterSettings, labels: PrintLabels): PrintDocument = printDocument {
        data.forEachIndexed { index, label ->
            repeat(label.copies.coerceIn(1, 50)) { copy ->
                if (index > 0 || copy > 0) {
                    feed(2)
                    cut()
                }
                text(label.name, TextStyle(alignment = PrintAlignment.CENTER, bold = true, heightScale = 2))
                label.secondaryName?.takeIf { it.isNotBlank() }?.let { center(it) }
                val price = money(label.price, settings.currencySymbol) + (label.unit?.let { " / $it" } ?: "")
                text(
                    "${labels.price}: $price",
                    TextStyle(alignment = PrintAlignment.CENTER, bold = true, widthScale = 2, heightScale = 2),
                )
                label.extraLine?.takeIf { it.isNotBlank() }?.let { center(it) }
                label.barcode?.takeIf { it.isNotBlank() }?.let { barcode(it, height = 60) }
            }
        }
    }

    fun inventory(data: InventoryReportData, settings: PrinterSettings, labels: PrintLabels): PrintDocument = printDocument {
        header(settings, labels, compact = true)
        title(if (data.lowStockOnly) labels.lowStockTitle else labels.inventoryTitle, scale = 1)
        center(data.generatedAt)
        divider()
        val wide = settings.charsPerLine >= WIDE_LAYOUT_MIN_CHARS
        row(
            Column(labels.item, weight = 1f),
            Column(labels.stock, fixedChars = 9, alignment = PrintAlignment.RIGHT),
            Column(labels.value, fixedChars = if (wide) 12 else 9, alignment = PrintAlignment.RIGHT),
            style = TextStyle(bold = true),
        )
        data.rows.groupBy { it.category }.forEach { (category, rows) ->
            divider('.')
            text(category, TextStyle(bold = true, underline = true))
            rows.forEach { row ->
                val name = if (row.isLowStock) "${labels.lowStockMark}${row.name}" else row.name
                row(
                    Column(name, weight = 1f),
                    Column(row.stock, fixedChars = 9, alignment = PrintAlignment.RIGHT),
                    Column(amount(row.value), fixedChars = if (wide) 12 else 9, alignment = PrintAlignment.RIGHT),
                )
            }
        }
        divider()
        keyValue(labels.totalItems, data.rows.size.toString())
        keyValue(labels.totalValue, money(data.totalValue, settings.currencySymbol), bold = true)
        feed()
    }

    /**
     * Exercises every style so the shop can check alignment, sizes, barcode and QR support.
     * [sampleLines] are printed once per style in [TEST_PAGE_STYLES].
     */
    fun testPage(
        settings: PrinterSettings,
        labels: PrintLabels,
        sampleLines: List<String>,
        barcodeData: String,
        qrData: String,
    ): PrintDocument = printDocument {
        header(settings, labels)
        title(labels.testPrintTitle, scale = 1)
        divider()
        text(CHARSET_SAMPLE)
        sampleLines.forEachIndexed { index, line ->
            text(line, TEST_PAGE_STYLES[index % TEST_PAGE_STYLES.size])
        }
        divider()
        barcode(barcodeData)
        feed()
        qr(settings.qrContent.ifBlank { qrData })
        footer(settings)
    }

    private const val CHARSET_SAMPLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789"

    private val TEST_PAGE_STYLES = listOf(
        TextStyle(),
        TextStyle(bold = true),
        TextStyle(underline = true),
        TextStyle(widthScale = 2, heightScale = 2),
        TextStyle(inverted = true),
        TextStyle(alignment = PrintAlignment.CENTER),
        TextStyle(alignment = PrintAlignment.RIGHT),
    )

    // ---------- shared blocks ----------

    fun PrintDocumentBuilder.header(settings: PrinterSettings, labels: PrintLabels, compact: Boolean = false) {
        val image = settings.image
        if (!compact && image.printLogo && image.logo != null) {
            image(image.logo, settings.headerFooter.headerAlignment)
            feed()
        }
        val hf = settings.headerFooter
        if (!hf.showHeader) return
        val align = hf.headerAlignment
        if (hf.shopName.isNotBlank()) {
            text(hf.shopName, TextStyle(alignment = align, bold = hf.boldShopName, widthScale = if (compact) 1 else 2, heightScale = 2))
        }
        if (compact) {
            divider()
            return
        }
        hf.addressLines.lines().filter { it.isNotBlank() }.forEach { text(it.trim(), TextStyle(alignment = align)) }
        if (hf.phone.isNotBlank()) text("${labels.phone}: ${hf.phone}", TextStyle(alignment = align))
        if (hf.taxId.isNotBlank()) text("${labels.taxId}: ${hf.taxId}", TextStyle(alignment = align))
        hf.headerNote.lines().filter { it.isNotBlank() }.forEach { text(it.trim(), TextStyle(alignment = align, bold = true)) }
        divider()
    }

    fun PrintDocumentBuilder.footer(settings: PrinterSettings) {
        val hf = settings.headerFooter
        if (hf.showFooter && hf.footerNote.isNotBlank()) {
            feed()
            hf.footerNote.lines().filter { it.isNotBlank() }.forEach { center(it.trim()) }
        }
        feed()
    }

    private fun PrintDocumentBuilder.items(
        items: List<PrintLineItem>,
        settings: PrinterSettings,
        labels: PrintLabels,
        showTax: Boolean = false,
    ) {
        val wide = settings.charsPerLine >= WIDE_LAYOUT_MIN_CHARS
        if (wide) {
            row(
                Column(labels.item, weight = 1f),
                Column(labels.quantity, fixedChars = 8, alignment = PrintAlignment.RIGHT),
                Column(labels.rate, fixedChars = 9, alignment = PrintAlignment.RIGHT),
                Column(labels.amount, fixedChars = 10, alignment = PrintAlignment.RIGHT),
                style = TextStyle(bold = true),
            )
            divider()
            items.forEach { item ->
                row(
                    Column(item.name, weight = 1f),
                    Column(item.quantity, fixedChars = 8, alignment = PrintAlignment.RIGHT),
                    Column(amount(item.unitPrice), fixedChars = 9, alignment = PrintAlignment.RIGHT),
                    Column(amount(item.total), fixedChars = 10, alignment = PrintAlignment.RIGHT),
                )
                extraLine(item, labels, showTax)
            }
        } else {
            row(
                Column(labels.item, weight = 1f),
                Column(labels.amount, fixedChars = 10, alignment = PrintAlignment.RIGHT),
                style = TextStyle(bold = true),
            )
            divider()
            items.forEach { item ->
                text(item.name, TextStyle(bold = settings.boldTitles))
                row(
                    Column("  ${item.quantity} x ${amount(item.unitPrice)}", weight = 1f),
                    Column(amount(item.total), fixedChars = 10, alignment = PrintAlignment.RIGHT),
                )
                extraLine(item, labels, showTax)
            }
        }
    }

    private fun PrintDocumentBuilder.extraLine(item: PrintLineItem, labels: PrintLabels, showTax: Boolean) {
        item.secondaryText?.takeIf { it.isNotBlank() }?.let { text("  $it") }
        if (showTax && item.taxPercent > 0.0) text("  ${labels.tax} ${amount(item.taxPercent)}%")
    }

    private fun padBetween(left: String, right: String, width: Int): String {
        val space = width - left.length - right.length
        return if (space >= 1) left + " ".repeat(space) + right else "$left $right"
    }

    fun money(value: Double, currency: String): String = "$currency${amount(value)}"

    /** Two-decimal formatting without platform `String.format`. */
    fun amount(value: Double): String {
        val cents = (abs(value) * 100).roundToLong()
        val sign = if (value < 0 && cents != 0L) "-" else ""
        val fraction = (cents % 100).toString().padStart(2, '0')
        return "$sign${cents / 100}.$fraction"
    }
}
