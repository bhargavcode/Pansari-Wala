package org.bhargav.pansariwala.printing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.feature.order.paymentMethodLabelRes
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.ANONYMOUS_CUSTOMER_KEY
import org.bhargav.pansariwala.i18n.WALK_IN_CUSTOMER_KEY
import org.bhargav.pansariwala.i18n.labelRes
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.printer.document.InventoryReportData
import org.bhargav.pansariwala.printer.document.InventoryRow
import org.bhargav.pansariwala.printer.document.InvoiceData
import org.bhargav.pansariwala.printer.document.LabelData
import org.bhargav.pansariwala.printer.document.OrderTicketData
import org.bhargav.pansariwala.printer.document.PrintDocument
import org.bhargav.pansariwala.printer.document.PrintLabels
import org.bhargav.pansariwala.printer.document.PrintLineItem
import org.bhargav.pansariwala.printer.document.PrintTemplates
import org.bhargav.pansariwala.printer.document.ReceiptData
import org.bhargav.pansariwala.printer.document.SalesReportData
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.pos_payment_split
import org.bhargav.pansariwala.util.asMoney
import pansariwala.shared.generated.resources.anonymous_customer
import pansariwala.shared.generated.resources.print_address
import pansariwala.shared.generated.resources.print_amount
import pansariwala.shared.generated.resources.print_bill_to
import pansariwala.shared.generated.resources.print_cashier
import pansariwala.shared.generated.resources.print_category
import pansariwala.shared.generated.resources.print_change
import pansariwala.shared.generated.resources.print_channel
import pansariwala.shared.generated.resources.print_channel_online
import pansariwala.shared.generated.resources.print_channel_pos
import pansariwala.shared.generated.resources.print_customer
import pansariwala.shared.generated.resources.print_date
import pansariwala.shared.generated.resources.print_discount
import pansariwala.shared.generated.resources.print_inventory_title
import pansariwala.shared.generated.resources.print_invoice_no
import pansariwala.shared.generated.resources.print_invoice_title
import pansariwala.shared.generated.resources.print_item
import pansariwala.shared.generated.resources.print_low_stock_mark
import pansariwala.shared.generated.resources.print_low_stock_title
import pansariwala.shared.generated.resources.print_note
import pansariwala.shared.generated.resources.print_order_no
import pansariwala.shared.generated.resources.print_paid
import pansariwala.shared.generated.resources.print_payment_mode
import pansariwala.shared.generated.resources.print_phone
import pansariwala.shared.generated.resources.print_price
import pansariwala.shared.generated.resources.print_qty
import pansariwala.shared.generated.resources.print_rate
import pansariwala.shared.generated.resources.print_receipt_title
import pansariwala.shared.generated.resources.print_status
import pansariwala.shared.generated.resources.print_stock
import pansariwala.shared.generated.resources.print_subtotal
import pansariwala.shared.generated.resources.print_tax
import pansariwala.shared.generated.resources.print_tax_id
import pansariwala.shared.generated.resources.print_test_line_bold
import pansariwala.shared.generated.resources.print_test_line_center
import pansariwala.shared.generated.resources.print_test_line_double
import pansariwala.shared.generated.resources.print_test_line_inverted
import pansariwala.shared.generated.resources.print_test_line_normal
import pansariwala.shared.generated.resources.print_test_line_right
import pansariwala.shared.generated.resources.print_test_line_underline
import pansariwala.shared.generated.resources.print_test_title
import pansariwala.shared.generated.resources.print_ticket_title
import pansariwala.shared.generated.resources.print_total
import pansariwala.shared.generated.resources.print_total_items
import pansariwala.shared.generated.resources.print_total_value
import pansariwala.shared.generated.resources.print_value
import pansariwala.shared.generated.resources.walk_in

enum class OrderDocument { RECEIPT, TICKET, INVOICE }

/** Thrown when a print was requested but there is no order / product to print. */
class NothingToPrintException : IllegalStateException()

/**
 * App-facing printing API: maps POS domain models to printer-module documents and sends them.
 * Screens call this; nothing here knows how bytes reach the printer.
 */
class PosPrintService(
    private val printer: PrinterManager,
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _results = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = RESULT_BUFFER)
    val results: SharedFlow<Result<Unit>> = _results.asSharedFlow()

    private val _activeJobs = MutableStateFlow(0)
    val activeJobs: StateFlow<Int> = _activeJobs.asStateFlow()

    private val settings: PrinterSettings get() = printer.settings.value

    suspend fun printTestPage(): Result<Unit> = print {
        PrintTemplates.testPage(
            settings = settings,
            labels = labels(),
            sampleLines = listOf(
                getString(Res.string.print_test_line_normal),
                getString(Res.string.print_test_line_bold),
                getString(Res.string.print_test_line_underline),
                getString(Res.string.print_test_line_double),
                getString(Res.string.print_test_line_inverted),
                getString(Res.string.print_test_line_center),
                getString(Res.string.print_test_line_right),
            ),
            barcodeData = TEST_BARCODE,
            qrData = AppConstants.WEB_BASE_URL,
        )
    }

    suspend fun printReceipt(orderId: String? = null): Result<Unit> = withOrder(orderId, OrderDocument.RECEIPT)

    suspend fun printOrderTicket(orderId: String? = null): Result<Unit> = withOrder(orderId, OrderDocument.TICKET)

    suspend fun printInvoice(orderId: String? = null): Result<Unit> = withOrder(orderId, OrderDocument.INVOICE)

    /** Prints an order already in memory (e.g. online orders fetched from the server). */
    suspend fun printOrder(order: Order, document: OrderDocument): Result<Unit> = print { documentFor(order, document) }

    private suspend fun documentFor(order: Order, document: OrderDocument): PrintDocument = when (document) {
        OrderDocument.RECEIPT -> PrintTemplates.receipt(order.toReceipt(), settings, labels())
        OrderDocument.TICKET -> PrintTemplates.orderTicket(
            OrderTicketData(
                orderNumber = order.displayNumber,
                dateTime = formatPrintDateTime(order.createdAtEpochMs),
                items = order.items.map { it.toLine() },
                customerName = customerName(order.customerName),
                channel = channelName(order.channel),
                deliveryAddress = order.deliveryAddress,
                note = order.dropoffInstructions,
            ),
            settings,
            labels(),
        )
        OrderDocument.INVOICE -> {
            val items = order.items.map { it.toLine() }
            PrintTemplates.invoice(
                InvoiceData(
                    invoiceNumber = order.displayNumber,
                    dateTime = formatPrintDateTime(order.createdAtEpochMs),
                    items = items,
                    total = order.totalValue,
                    subtotal = items.sumOf { it.total },
                    billToName = customerName(order.customerName),
                    billToAddress = order.deliveryAddress,
                    billToPhone = order.customerPhone,
                    paymentMode = paymentLabel(order),
                ),
                settings,
                labels(),
            )
        }
    }

    /** Shelf labels for [products]; defaults to the first few catalogue items as a sample. */
    suspend fun printLabels(products: List<Product>? = null): Result<Unit> {
        val list = products ?: shopRepository.observeProducts(shopId()).first().take(AppConstants.PRINT_SAMPLE_LABEL_LIMIT)
        if (list.isEmpty()) return Result.failure(NothingToPrintException())
        val data = list.map { product ->
            LabelData(
                name = product.name,
                secondaryName = product.nameHi.takeIf { it.isNotBlank() },
                price = product.sellingPrice,
                unit = getString(product.unit.labelRes),
                barcode = product.barcode,
            )
        }
        return print { PrintTemplates.labels(data, settings, labels()) }
    }

    suspend fun printInventory(lowStockOnly: Boolean): Result<Unit> {
        val products = shopRepository.observeProducts(shopId()).first()
            .filter { !lowStockOnly || it.isLowStock }
        if (products.isEmpty()) return Result.failure(NothingToPrintException())
        val rows = products.sortedBy { it.name.lowercase() }.map { product ->
            InventoryRow(
                name = product.name,
                category = getString(product.category.labelRes),
                stock = "${product.stockQty.asQuantity()} ${getString(product.unit.labelRes)}",
                value = product.stockValue,
                isLowStock = product.isLowStock,
            )
        }
        return print {
            PrintTemplates.inventory(
                InventoryReportData(
                    generatedAt = formatPrintDateTime(AppClock.nowMillis()),
                    rows = rows,
                    lowStockOnly = lowStockOnly,
                ),
                settings,
                labels(),
            )
        }
    }

    suspend fun printSalesReport(data: SalesReportData): Result<Unit> {
        if (data.summary.isEmpty() && data.rows.isEmpty()) return Result.failure(NothingToPrintException())
        return print { PrintTemplates.salesReport(data, settings, labels()) }
    }

    /**
     * Runs a print in the app-wide scope so it finishes even if the screen closes. The outcome is
     * published on [results] for a single place in the UI to report.
     */
    fun enqueue(job: suspend PosPrintService.() -> Result<Unit>) {
        background.launch {
            _activeJobs.update { it + 1 }
            try {
                _results.emit(job())
            } finally {
                _activeJobs.update { it - 1 }
            }
        }
    }

    /** Receipt after checkout: always when [printRequested], otherwise only if auto print is on. */
    fun onOrderSaved(order: Order, printRequested: Boolean = false) {
        val auto = settings.autoPrintReceipt && settings.lastDevice != null
        if (!printRequested && !auto) return
        enqueue { printOrder(order, OrderDocument.RECEIPT) }
    }

    private suspend fun withOrder(orderId: String?, document: OrderDocument): Result<Unit> {
        val id = orderId ?: shopRepository.observeRecentOrders(shopId(), 1).first().firstOrNull()?.id
        val order = id?.let { shopRepository.getOrder(it) } ?: return Result.failure(NothingToPrintException())
        return printOrder(order, document)
    }

    private suspend fun print(build: suspend () -> PrintDocument): Result<Unit> =
        runCatching { build() }.fold(
            onSuccess = { printer.print(it) },
            onFailure = { Result.failure(it) },
        )

    private suspend fun Order.toReceipt(): ReceiptData {
        val items = items.map { it.toLine() }
        val subtotal = items.sumOf { it.total }
        return ReceiptData(
            orderNumber = displayNumber,
            dateTime = formatPrintDateTime(createdAtEpochMs),
            items = items,
            total = totalValue,
            subtotal = subtotal,
            discount = (subtotal - totalValue).coerceAtLeast(0.0),
            paymentMode = paymentLabel(this),
            customerName = customerName(customerName),
        )
    }

    private suspend fun OrderItem.toLine(): PrintLineItem = PrintLineItem(
        name = productName,
        quantity = "${quantity.asQuantity()} ${getString(unit.labelRes)}",
        unitPrice = unitPrice,
        total = lineTotal,
    )

    private suspend fun customerName(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> null
            trimmed.equals(WALK_IN_CUSTOMER_KEY, ignoreCase = true) -> getString(Res.string.walk_in)
            trimmed.equals(ANONYMOUS_CUSTOMER_KEY, ignoreCase = true) -> getString(Res.string.anonymous_customer)
            else -> trimmed
        }
    }

    private suspend fun paymentLabel(order: Order): String? {
        val method = order.paymentMethod.takeIf { it.isNotBlank() } ?: return null
        val label = getString(paymentMethodLabelRes(method))
        if (!order.isPartialUdhaar) return label
        return getString(
            Res.string.pos_payment_split,
            getString(paymentMethodLabelRes(order.paidNowMethod ?: AppConstants.PosPayment.CASH)),
            order.paidNow.asMoney(),
            label,
            order.udhaarAmount.asMoney(),
        )
    }

    private suspend fun channelName(channel: OrderChannel): String = when (channel) {
        OrderChannel.POS -> getString(Res.string.print_channel_pos)
        OrderChannel.ONLINE -> getString(Res.string.print_channel_online)
    }

    private suspend fun shopId(): String = preferences.getShopId() ?: AppConstants.DEMO_SHOP_ID

    private suspend fun labels(): PrintLabels = PrintLabels(
        receiptTitle = getString(Res.string.print_receipt_title),
        invoiceTitle = getString(Res.string.print_invoice_title),
        orderTicketTitle = getString(Res.string.print_ticket_title),
        inventoryTitle = getString(Res.string.print_inventory_title),
        lowStockTitle = getString(Res.string.print_low_stock_title),
        testPrintTitle = getString(Res.string.print_test_title),
        orderNumber = getString(Res.string.print_order_no),
        invoiceNumber = getString(Res.string.print_invoice_no),
        date = getString(Res.string.print_date),
        customer = getString(Res.string.print_customer),
        cashier = getString(Res.string.print_cashier),
        item = getString(Res.string.print_item),
        quantity = getString(Res.string.print_qty),
        rate = getString(Res.string.print_rate),
        amount = getString(Res.string.print_amount),
        subtotal = getString(Res.string.print_subtotal),
        discount = getString(Res.string.print_discount),
        tax = getString(Res.string.print_tax),
        total = getString(Res.string.print_total),
        paid = getString(Res.string.print_paid),
        change = getString(Res.string.print_change),
        paymentMode = getString(Res.string.print_payment_mode),
        billTo = getString(Res.string.print_bill_to),
        phone = getString(Res.string.print_phone),
        taxId = getString(Res.string.print_tax_id),
        address = getString(Res.string.print_address),
        note = getString(Res.string.print_note),
        channel = getString(Res.string.print_channel),
        status = getString(Res.string.print_status),
        category = getString(Res.string.print_category),
        stock = getString(Res.string.print_stock),
        value = getString(Res.string.print_value),
        lowStockMark = getString(Res.string.print_low_stock_mark),
        totalItems = getString(Res.string.print_total_items),
        totalValue = getString(Res.string.print_total_value),
        price = getString(Res.string.print_price),
    )

    private companion object {
        const val TEST_BARCODE = "PANSARI123"
        const val RESULT_BUFFER = 8
    }
}
