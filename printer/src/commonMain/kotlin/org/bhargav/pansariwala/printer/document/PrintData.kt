package org.bhargav.pansariwala.printer.document

/**
 * Plain data handed to [PrintTemplates]. Callers map their domain models into these so the printer
 * module never depends on app types.
 */
data class PrintLineItem(
    val name: String,
    val quantity: String,
    val unitPrice: Double,
    val total: Double,
    val taxPercent: Double = 0.0,
    val secondaryText: String? = null,
)

data class ReceiptData(
    val orderNumber: String,
    val dateTime: String,
    val items: List<PrintLineItem>,
    val total: Double,
    val subtotal: Double = items.sumOf { it.total },
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val paid: Double? = null,
    val change: Double? = null,
    val paymentMode: String? = null,
    val customerName: String? = null,
    val cashier: String? = null,
    val note: String? = null,
)

data class OrderTicketData(
    val orderNumber: String,
    val dateTime: String,
    val items: List<PrintLineItem>,
    val customerName: String? = null,
    val channel: String? = null,
    val status: String? = null,
    val deliveryAddress: String? = null,
    val note: String? = null,
)

data class InvoiceData(
    val invoiceNumber: String,
    val dateTime: String,
    val items: List<PrintLineItem>,
    val total: Double,
    val subtotal: Double = items.sumOf { it.total },
    val taxTotal: Double = 0.0,
    val discount: Double = 0.0,
    val billToName: String? = null,
    val billToAddress: String? = null,
    val billToPhone: String? = null,
    val paymentMode: String? = null,
    val terms: String? = null,
)

data class LabelData(
    val name: String,
    val price: Double,
    val secondaryName: String? = null,
    val unit: String? = null,
    val barcode: String? = null,
    val extraLine: String? = null,
    val copies: Int = 1,
)

data class InventoryRow(
    val name: String,
    val category: String,
    val stock: String,
    val value: Double,
    val isLowStock: Boolean = false,
)

data class InventoryReportData(
    val generatedAt: String,
    val rows: List<InventoryRow>,
    val totalValue: Double = rows.sumOf { it.value },
    val lowStockOnly: Boolean = false,
)

/** Every fixed word printed by the templates, supplied by the app so it stays localizable. */
data class PrintLabels(
    val receiptTitle: String,
    val invoiceTitle: String,
    val orderTicketTitle: String,
    val inventoryTitle: String,
    val lowStockTitle: String,
    val testPrintTitle: String,
    val orderNumber: String,
    val invoiceNumber: String,
    val date: String,
    val customer: String,
    val cashier: String,
    val item: String,
    val quantity: String,
    val rate: String,
    val amount: String,
    val subtotal: String,
    val discount: String,
    val tax: String,
    val total: String,
    val paid: String,
    val change: String,
    val paymentMode: String,
    val billTo: String,
    val phone: String,
    val taxId: String,
    val address: String,
    val note: String,
    val channel: String,
    val status: String,
    val category: String,
    val stock: String,
    val value: String,
    val lowStockMark: String,
    val totalItems: String,
    val totalValue: String,
    val price: String,
)
