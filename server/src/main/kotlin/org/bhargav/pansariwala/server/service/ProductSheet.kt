package org.bhargav.pansariwala.server.service

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.bhargav.pansariwala.server.dto.ProductVariantDto
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.UUID

/**
 * One spreadsheet layout for products + variants, shared by shop inventory and the master catalog.
 * One row per variant; rows with the same product_id (or the same name + brand) form one product.
 * A row without variant_label/variant_id describes the product itself (items sold without variants).
 */
object SheetCol {
    const val PRODUCT_ID = "product_id"
    const val NAME = "name"
    const val NAME_HI = "name_hi"
    const val BRAND = "brand"
    const val CATEGORY = "category"
    const val UNIT = "unit"
    const val BARCODE = "barcode"
    const val VARIANT_ID = "variant_id"
    const val VARIANT_LABEL = "variant_label"
    const val SELL_MODE = "sell_mode"
    const val PACK_SIZE = "pack_size"
    const val PACK_UNIT = "pack_unit"
    const val BUNDLE_COUNT = "bundle_count"
    const val BUNDLE_OF = "bundle_of"
    const val PRICE = "price"
    const val MRP = "mrp"
    const val COST_PRICE = "cost_price"
    const val STOCK = "stock"
    const val STOCK_ADD = "stock_add"
    const val LOW_STOCK_ALERT = "low_stock_alert"
    const val VARIANT_BARCODE = "variant_barcode"
    const val SKU = "sku"
    const val MIN_QTY = "min_qty"
    const val STEP_QTY = "step_qty"
    const val ACTIVE = "active"
    const val IMAGE_URL = "image_url"
    const val VOICE_ALIAS = "voice_alias"
    const val DESCRIPTION = "description"
}

/** Column order and the help text written to the Instructions sheet. */
val SHEET_COLUMNS: List<Pair<String, String>> = listOf(
    SheetCol.PRODUCT_ID to "Leave blank for a new product. Keep the value from an export to update that product.",
    SheetCol.NAME to "Required for new products. Product / brand line name without the pack size, e.g. Parle-G Biscuit.",
    SheetCol.NAME_HI to "Hindi name (optional).",
    SheetCol.BRAND to "Brand, e.g. Parle. Name + brand identify a product when product_id is blank.",
    SheetCol.CATEGORY to "Shop: FORTUNE_BRANDED, GENERAL_GROCERY, PUJA_SAMAGRI, STANDARD_SPICES, HIGH_VALUE_SPICES. Master: category name (created if missing).",
    SheetCol.UNIT to "Product unit: PIECE, PACKET, KG, GRAM or LITRE. Use PIECE for packaged goods; KG/LITRE only for loose items without variants.",
    SheetCol.BARCODE to "Product barcode (optional). Variant barcodes go in variant_barcode.",
    SheetCol.VARIANT_ID to "Leave blank for a new variant. Keep the value from an export to update that variant.",
    SheetCol.VARIANT_LABEL to "Variant name shown to customers, e.g. Small, 100 g, Carton of 24. Blank = product without variants.",
    SheetCol.SELL_MODE to "PACK (sold per packet), LOOSE (sold by weight/volume, khula) or BUNDLE (several packs together, e.g. carton).",
    SheetCol.PACK_SIZE to "Net content of one pack, e.g. 100. Information only; does not change how quantity is counted.",
    SheetCol.PACK_UNIT to "Unit of pack_size: GRAM, KG, LITRE or PIECE.",
    SheetCol.BUNDLE_COUNT to "BUNDLE only: packs inside one bundle, e.g. 24.",
    SheetCol.BUNDLE_OF to "BUNDLE only (optional): variant_label of the pack this bundle takes stock from. Selling 1 bundle then reduces that pack's stock by bundle_count.",
    SheetCol.PRICE to "Selling price per pack, or per unit (kg / ltr) for LOOSE. Required for new products and variants.",
    SheetCol.MRP to "MRP printed on the pack (optional).",
    SheetCol.COST_PRICE to "Purchase price (product level, optional).",
    SheetCol.STOCK to "Set stock to this number (packs, or kg / ltr for LOOSE). Leave blank to keep the current stock.",
    SheetCol.STOCK_ADD to "Add this quantity to current stock, e.g. new stock received. Can be combined with blank stock.",
    SheetCol.LOW_STOCK_ALERT to "Low-stock alert level (product level, optional).",
    SheetCol.VARIANT_BARCODE to "Barcode printed on this variant's pack.",
    SheetCol.SKU to "Your own code (optional).",
    SheetCol.MIN_QTY to "LOOSE only: minimum amount, e.g. 0.1 for 100 g.",
    SheetCol.STEP_QTY to "LOOSE only: step amount, e.g. 0.05 for 50 g.",
    SheetCol.ACTIVE to "YES or NO. NO hides the variant (or the product on a product row) from billing and customers.",
    SheetCol.IMAGE_URL to "Image link (optional).",
    SheetCol.VOICE_ALIAS to "Shop only: extra names for voice billing, comma separated (optional).",
    SheetCol.DESCRIPTION to "Master only: description (optional).",
)

private val TEXT_COLUMNS = setOf(SheetCol.PRODUCT_ID, SheetCol.VARIANT_ID, SheetCol.BARCODE, SheetCol.VARIANT_BARCODE, SheetCol.SKU)
private val SELL_MODES = setOf("PACK", "LOOSE", "BUNDLE")
private val UNITS = setOf("PIECE", "PACKET", "KG", "GRAM", "LITRE")
private const val MAX_ROWS = 20_000

data class SheetRow(val rowNumber: Int, val cells: Map<String, String>) {
    fun text(col: String): String = cells[col]?.trim().orEmpty()
    fun number(col: String): Double? = text(col).replace(",", "").replace("₹", "").trim().toDoubleOrNull()
    fun flag(col: String): Boolean? = when (text(col).lowercase()) {
        "yes", "y", "true", "1", "active" -> true
        "no", "n", "false", "0", "inactive" -> false
        else -> null
    }
    val isVariantRow: Boolean get() = text(SheetCol.VARIANT_LABEL).isNotBlank() || text(SheetCol.VARIANT_ID).isNotBlank()
}

/** Rows of one product: the first non-blank value wins for product-level columns. */
data class SheetProduct(val rows: List<SheetRow>) {
    val firstRow: Int get() = rows.first().rowNumber
    fun text(col: String): String = rows.firstNotNullOfOrNull { it.text(col).ifBlank { null } }.orEmpty()
    fun number(col: String): Double? = rows.firstNotNullOfOrNull { it.number(col) }
    val variantRows: List<SheetRow> get() = rows.filter { it.isVariantRow }
    val productRow: SheetRow? get() = rows.firstOrNull { !it.isVariantRow }
    val unit: String? get() = text(SheetCol.UNIT).uppercase().ifBlank { null }?.also {
        require(it in UNITS) { "Row $firstRow: unknown unit '$it'" }
    }
    /** Every barcode in the group, for matching existing products. */
    val barcodes: List<String> get() =
        rows.flatMap { listOf(it.text(SheetCol.BARCODE), it.text(SheetCol.VARIANT_BARCODE)) }.filter { it.isNotBlank() }.distinct()
}

data class VariantMergeResult(val variants: List<ProductVariantDto>, val created: Int, val updated: Int)

object ProductSheet {
    fun nameKey(name: String, brand: String): String =
        "${name.trim().lowercase().replace(Regex("\\s+"), " ")}|${brand.trim().lowercase()}"

    fun read(bytes: ByteArray, fileName: String): List<SheetRow> {
        val table = if (fileName.lowercase().endsWith(".csv")) readCsv(bytes) else readWorkbook(bytes)
        val headerIndex = table.indexOfFirst { row -> row.any { it.isNotBlank() } }
        require(headerIndex >= 0) { "The sheet is empty" }
        val header = table[headerIndex].map(::normalizeHeader)
        require(SheetCol.NAME in header || SheetCol.PRODUCT_ID in header) {
            "Header row must contain '${SheetCol.NAME}' or '${SheetCol.PRODUCT_ID}'. Download the sample sheet for the layout."
        }
        val rows = table.drop(headerIndex + 1).mapIndexedNotNull { i, values ->
            if (values.all { it.isBlank() }) return@mapIndexedNotNull null
            val cells = header.mapIndexedNotNull { c, col -> if (col.isBlank()) null else col to values.getOrElse(c) { "" } }.toMap()
            SheetRow(rowNumber = headerIndex + i + 2, cells = cells)
        }
        require(rows.size <= MAX_ROWS) { "Too many rows (max $MAX_ROWS)" }
        return rows
    }

    fun group(rows: List<SheetRow>): List<SheetProduct> {
        val groups = LinkedHashMap<String, MutableList<SheetRow>>()
        var lastKey: String? = null
        rows.forEach { row ->
            val id = row.text(SheetCol.PRODUCT_ID)
            val name = row.text(SheetCol.NAME)
            val key = when {
                id.isNotBlank() -> "id:$id"
                name.isNotBlank() -> "name:" + nameKey(name, row.text(SheetCol.BRAND))
                // Variant rows may leave name blank under their product row.
                row.isVariantRow && lastKey != null -> lastKey
                else -> error("Row ${row.rowNumber}: '${SheetCol.NAME}' or '${SheetCol.PRODUCT_ID}' is required")
            }
            groups.getOrPut(key) { mutableListOf() } += row
            lastKey = key
        }
        return groups.values.map { SheetProduct(it) }
    }

    /**
     * Applies variant rows onto [current]. Matching: variant_id, then variant_barcode, then label.
     * Variants not mentioned stay as they are, so a stock-only sheet never removes options.
     */
    fun mergeVariants(current: List<ProductVariantDto>, rows: List<SheetRow>): VariantMergeResult {
        val result = current.toMutableList()
        var created = 0
        var updated = 0
        val bundleLinks = mutableListOf<Pair<Int, String>>()
        rows.forEach { row ->
            val id = row.text(SheetCol.VARIANT_ID)
            val barcode = row.text(SheetCol.VARIANT_BARCODE)
            val label = row.text(SheetCol.VARIANT_LABEL)
            val index = result.indexOfFirst { id.isNotBlank() && it.id == id }
                .takeIf { it >= 0 } ?: result.indexOfFirst { barcode.isNotBlank() && it.barcode == barcode }
                .takeIf { it >= 0 } ?: result.indexOfFirst { label.isNotBlank() && it.name.equals(label, ignoreCase = true) }
            val base = if (index >= 0) result[index] else {
                require(label.isNotBlank()) { "Row ${row.rowNumber}: variant_label is required for a new variant" }
                require((row.number(SheetCol.PRICE) ?: 0.0) > 0.0) { "Row ${row.rowNumber}: price is required for new variant '$label'" }
                ProductVariantDto(name = label, id = id.ifBlank { "v_${UUID.randomUUID().toString().take(8)}" })
            }
            val mode = row.text(SheetCol.SELL_MODE).uppercase().ifBlank { base.sellMode }
            require(mode in SELL_MODES) { "Row ${row.rowNumber}: sell_mode must be PACK, LOOSE or BUNDLE" }
            val packUnit = row.text(SheetCol.PACK_UNIT).uppercase().ifBlank { base.packUnit }
            val setStock = row.number(SheetCol.STOCK)
            val addStock = row.number(SheetCol.STOCK_ADD) ?: 0.0
            val next = base.copy(
                name = label.ifBlank { base.name },
                sellMode = mode,
                price = row.number(SheetCol.PRICE) ?: base.price,
                mrp = row.number(SheetCol.MRP) ?: base.mrp,
                stockQty = ((setStock ?: base.stockQty) + addStock).coerceAtLeast(0.0),
                packSize = row.number(SheetCol.PACK_SIZE) ?: base.packSize,
                packUnit = packUnit,
                bundleCount = row.number(SheetCol.BUNDLE_COUNT)?.toInt() ?: base.bundleCount,
                barcode = barcode.ifBlank { null } ?: base.barcode,
                sku = row.text(SheetCol.SKU).ifBlank { base.sku },
                minQty = row.number(SheetCol.MIN_QTY) ?: base.minQty,
                stepQty = row.number(SheetCol.STEP_QTY) ?: base.stepQty,
                baseUnit = if (mode == "LOOSE") base.baseUnit.ifBlank { "KG" } else base.baseUnit,
                active = row.flag(SheetCol.ACTIVE) ?: base.active,
                imageUrls = row.text(SheetCol.IMAGE_URL).ifBlank { null }?.let { listOf(it) } ?: base.imageUrls,
            )
            val at = if (index >= 0) { result[index] = next; updated++; index } else { result += next; created++; result.lastIndex }
            row.text(SheetCol.BUNDLE_OF).takeIf { it.isNotBlank() }?.let { bundleLinks += at to it }
        }
        bundleLinks.forEach { (at, baseLabel) ->
            val base = result.firstOrNull { it.name.equals(baseLabel, ignoreCase = true) && it.sellMode == "PACK" }
                ?: error("bundle_of '$baseLabel' does not match a PACK variant label of '${result[at].name}'s product")
            result[at] = result[at].copy(baseVariantId = base.id)
        }
        return VariantMergeResult(result, created, updated)
    }

    fun template(): ByteArray = write(
        listOf(
            sample("Parle-G Biscuit", "पारले-जी बिस्किट", "Parle", "GENERAL_GROCERY", "Small", "PACK", 25.0, "GRAM", price = 5.0, mrp = 5.0, stock = 120.0, barcode = "8901719110016"),
            sample("Parle-G Biscuit", "", "Parle", "", "Medium", "PACK", 100.0, "GRAM", price = 10.0, mrp = 10.0, stock = 80.0),
            sample("Parle-G Biscuit", "", "Parle", "", "Large", "PACK", 500.0, "GRAM", price = 50.0, mrp = 50.0, stockAdd = 24.0),
            sample("Parle-G Biscuit", "", "Parle", "", "Carton of 24 Medium", "BUNDLE", 0.0, "", price = 230.0, mrp = 240.0, bundleCount = 24, bundleOf = "Medium"),
            sample("Sugar", "चीनी", "", "GENERAL_GROCERY", "Loose", "LOOSE", 0.0, "", price = 44.0, stock = 50.0, minQty = 0.25, stepQty = 0.25),
            sample("Sugar", "", "", "", "1 kg pack", "PACK", 1.0, "KG", price = 48.0, mrp = 52.0, stock = 30.0),
            mapOf(
                SheetCol.NAME to "Agarbatti Stand", SheetCol.UNIT to "PIECE", SheetCol.CATEGORY to "PUJA_SAMAGRI",
                SheetCol.PRICE to 35.0, SheetCol.STOCK to 12.0, SheetCol.ACTIVE to "YES",
            ),
        ),
    )

    private fun sample(
        name: String, nameHi: String, brand: String, category: String, label: String, mode: String,
        packSize: Double, packUnit: String, price: Double, mrp: Double = 0.0, stock: Double? = null, stockAdd: Double? = null,
        bundleCount: Int = 0, bundleOf: String = "", minQty: Double = 0.0, stepQty: Double = 0.0, barcode: String = "",
    ): Map<String, Any?> = mapOf(
        SheetCol.NAME to name, SheetCol.NAME_HI to nameHi, SheetCol.BRAND to brand, SheetCol.CATEGORY to category,
        SheetCol.UNIT to if (category.isNotBlank()) "PIECE" else "",
        SheetCol.VARIANT_LABEL to label, SheetCol.SELL_MODE to mode,
        SheetCol.PACK_SIZE to packSize.takeIf { it > 0 }, SheetCol.PACK_UNIT to packUnit,
        SheetCol.BUNDLE_COUNT to bundleCount.takeIf { it > 0 }, SheetCol.BUNDLE_OF to bundleOf,
        SheetCol.PRICE to price, SheetCol.MRP to mrp.takeIf { it > 0 }, SheetCol.STOCK to stock, SheetCol.STOCK_ADD to stockAdd,
        SheetCol.MIN_QTY to minQty.takeIf { it > 0 }, SheetCol.STEP_QTY to stepQty.takeIf { it > 0 },
        SheetCol.VARIANT_BARCODE to barcode, SheetCol.ACTIVE to "YES",
    )

    /** Rows keyed by [SheetCol] names; numbers stay numeric cells, ids/barcodes are text cells. */
    fun write(rows: List<Map<String, Any?>>): ByteArray = XSSFWorkbook().use { book ->
        val headerStyle = book.createCellStyle().apply {
            setFont(book.createFont().apply { bold = true })
            fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val textStyle = book.createCellStyle().apply { dataFormat = book.createDataFormat().getFormat("@") }
        val sheet = book.createSheet("Products")
        val header = sheet.createRow(0)
        SHEET_COLUMNS.forEachIndexed { i, (col, _) ->
            header.createCell(i).apply { setCellValue(col); cellStyle = headerStyle }
            if (col in TEXT_COLUMNS) sheet.setDefaultColumnStyle(i, textStyle)
        }
        rows.forEachIndexed { r, values ->
            val row = sheet.createRow(r + 1)
            SHEET_COLUMNS.forEachIndexed { i, (col, _) ->
                when (val v = values[col]) {
                    null -> Unit
                    is Number -> row.createCell(i).setCellValue(v.toDouble())
                    is Boolean -> row.createCell(i).setCellValue(if (v) "YES" else "NO")
                    else -> v.toString().takeIf { it.isNotEmpty() }?.let {
                        row.createCell(i).apply { setCellValue(it); if (col in TEXT_COLUMNS) cellStyle = textStyle }
                    }
                }
            }
        }
        sheet.createFreezePane(0, 1)
        SHEET_COLUMNS.indices.forEach { sheet.setColumnWidth(it, 18 * 256) }
        val help = book.createSheet("Instructions")
        help.createRow(0).apply {
            createCell(0).apply { setCellValue("column"); cellStyle = headerStyle }
            createCell(1).apply { setCellValue("how to fill"); cellStyle = headerStyle }
        }
        SHEET_COLUMNS.forEachIndexed { i, (col, text) ->
            help.createRow(i + 1).apply { createCell(0).setCellValue(col); createCell(1).setCellValue(text) }
        }
        listOf(
            "One row per variant. Repeat the product name (or product_id) on each variant row, or leave name blank right below the product's first row.",
            "Only filled cells change. Blank cells keep the current value, so a sheet with product_id, variant_label and stock_add only updates stock.",
            "Variants are never deleted by an import. Set active = NO to hide one.",
        ).forEachIndexed { i, note -> help.createRow(SHEET_COLUMNS.size + 2 + i).createCell(0).setCellValue(note) }
        help.setColumnWidth(0, 20 * 256)
        help.setColumnWidth(1, 110 * 256)
        ByteArrayOutputStream().also { book.write(it) }.toByteArray()
    }

    private fun normalizeHeader(raw: String): String =
        raw.lowercase().substringBefore('(').replace("*", "").trim().replace(Regex("[\\s\\-]+"), "_")

    private fun readWorkbook(bytes: ByteArray): List<List<String>> =
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { book ->
            val sheet = book.getSheet("Products") ?: book.getSheetAt(0)
            val width = (sheet.firstOrNull { it.physicalNumberOfCells > 0 }?.lastCellNum?.toInt() ?: 0).coerceAtLeast(1)
            (0..sheet.lastRowNum).map { r ->
                val row: Row? = sheet.getRow(r)
                (0 until maxOf(width, row?.lastCellNum?.toInt() ?: 0)).map { c -> row?.getCell(c)?.let(::cellText).orEmpty() }
            }
        }

    private fun cellText(cell: Cell): String {
        val type = if (cell.cellType == CellType.FORMULA) cell.cachedFormulaResultType else cell.cellType
        return when (type) {
            CellType.NUMERIC ->
                if (DateUtil.isCellDateFormatted(cell)) cell.localDateTimeCellValue.toString()
                else BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
            CellType.STRING -> cell.stringCellValue
            CellType.BOOLEAN -> if (cell.booleanCellValue) "YES" else "NO"
            else -> ""
        }.trim()
    }

    /** RFC-4180-ish: quoted fields, doubled quotes, commas and newlines inside quotes. */
    private fun readCsv(bytes: ByteArray): List<List<String>> {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> { row += field.toString(); field.clear() }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += field.toString(); field.clear(); rows += row; row = mutableListOf()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
