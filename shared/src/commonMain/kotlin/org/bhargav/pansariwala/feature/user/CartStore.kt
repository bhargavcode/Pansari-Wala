package org.bhargav.pansariwala.feature.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.util.AppConstants.LooseQty
import kotlin.math.round

/** Quantity rules for one purchasable option: a [variant], or the bare [product] when it has none. */
data class PurchaseOption(val product: Product, val variant: ProductVariant?) {
    val key: String get() = CartStore.lineKey(product.id, variant?.id)
    /** Shop price before the product offer. */
    val basePrice: Double get() = variant?.price ?: product.sellingPrice
    /** Price charged per unit: [basePrice] after the product offer. */
    val unitPrice: Double get() = product.salePrice(basePrice)
    val mrp: Double get() = variant?.mrp ?: 0.0
    /** Struck-through "was" price: the higher of MRP and [basePrice], shown only when above [unitPrice]. */
    val strikePrice: Double? get() = maxOf(mrp, basePrice).takeIf { it > unitPrice + LooseQty.EPSILON }
    val isLoose: Boolean get() = variant?.isLoose ?: product.isLooseUnit

    /** Unit that cart quantities are measured in (kg for loose sugar, pack count otherwise). */
    val quantityUnit: ProductUnit get() = when {
        variant?.isLoose == true -> variant.baseUnit
        variant != null -> ProductUnit.PIECE
        else -> product.unit
    }
    val imageUrl: String? get() = variant?.primaryImageUrl ?: product.primaryImageUrl
    val stock: Double get() = variant?.stockQty ?: product.stockQty
    val inStock: Boolean get() = stock >= minQty - LooseQty.EPSILON && stock > 0.0

    val step: Double get() = when {
        !isLoose -> 1.0
        (variant?.stepQty ?: 0.0) > 0.0 -> variant!!.stepQty
        quantityUnit == ProductUnit.GRAM -> LooseQty.STEP_GRAM
        else -> LooseQty.STEP_BASE_UNIT
    }
    val minQty: Double get() = when {
        !isLoose -> 1.0
        (variant?.minQty ?: 0.0) > 0.0 -> variant!!.minQty
        else -> step
    }

    /** Upper bound from the variant setting and stock; never below [minQty]. */
    val maxQty: Double get() {
        val configured = variant?.maxQty?.takeIf { it > 0.0 } ?: Double.MAX_VALUE
        return minOf(configured, stock).coerceAtLeast(minQty)
    }
    val quickPicks: List<Double> get() = when {
        !isLoose -> emptyList()
        !variant?.quickPicks.isNullOrEmpty() -> variant!!.quickPicks
        quantityUnit == ProductUnit.GRAM -> LooseQty.QUICK_PICKS_GRAM
        else -> LooseQty.QUICK_PICKS_BASE_UNIT
    }.filter { it + LooseQty.EPSILON >= minQty && it <= maxQty + LooseQty.EPSILON }

    val defaultQty: Double get() = when {
        !isLoose -> 1.0
        quantityUnit == ProductUnit.GRAM -> LooseQty.DEFAULT_GRAM
        else -> LooseQty.DEFAULT_BASE_UNIT
    }.coerceIn(minQty, maxQty)

    /** Clamps to [minQty]..[maxQty] and snaps to [step] so totals stay clean (0.25, not 0.2499). */
    fun normalize(quantity: Double): Double {
        val snapped = round(quantity / step) * step
        val rounded = round(snapped * 1000.0) / 1000.0
        return rounded.coerceIn(minQty, maxQty)
    }

    fun priceFor(quantity: Double): Double = unitPrice * quantity
}

fun Product.options(): List<PurchaseOption> =
    if (variants.isEmpty()) listOf(PurchaseOption(this, null)) else variants.map { PurchaseOption(this, it) }

/** Products needing a choice (variants or a weight) open the picker instead of a one-tap "+". */
val Product.needsPicker: Boolean get() = variants.size > 1 || options().first().isLoose

class CartStore {
    data class Line(val product: Product, val variant: ProductVariant?, val quantity: Double) {
        val option: PurchaseOption get() = PurchaseOption(product, variant)
        val key: String get() = option.key
        val unitPrice: Double get() = option.unitPrice
        val lineTotal: Double get() = unitPrice * quantity
        /** Line total before the product offer; equals [lineTotal] when there is no offer. */
        val baseLineTotal: Double get() = option.basePrice * quantity
        val isLoose: Boolean get() = option.isLoose
    }

    private val _shopId = MutableStateFlow<String?>(null)
    val shopId: StateFlow<String?> = _shopId.asStateFlow()

    private val _shopName = MutableStateFlow<String?>(null)
    val shopName: StateFlow<String?> = _shopName.asStateFlow()

    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines.asStateFlow()

    fun bindShop(shopId: String, shopName: String? = null) {
        if (_shopId.value != shopId) {
            _shopId.value = shopId
            _shopName.value = shopName
            _lines.value = emptyList()
        } else if (shopName != null) {
            _shopName.value = shopName
        }
    }

    /** One-tap add for simple products: the only option at its default quantity. */
    fun add(product: Product) {
        val option = product.options().first()
        add(option, option.defaultQty)
    }

    /** Adds [quantity] of [option], merging with an existing line for the same option. */
    fun add(option: PurchaseOption, quantity: Double) {
        bindShop(option.product.shopId)
        _lines.update { current ->
            val existing = current.firstOrNull { it.key == option.key }
            if (existing == null) current + Line(option.product, option.variant, option.normalize(quantity))
            else current.map { if (it.key == option.key) it.copy(quantity = option.normalize(it.quantity + quantity)) else it }
        }
    }

    /**
     * Replaces the line [key] with [option] at exactly [quantity] (variant or amount edited from the cart).
     * Merges into another line if the new option is already in the cart.
     */
    fun update(key: String, option: PurchaseOption, quantity: Double) {
        bindShop(option.product.shopId)
        _lines.update { current ->
            val index = current.indexOfFirst { it.key == key }
            val line = Line(option.product, option.variant, option.normalize(quantity))
            val others = current.filterNot { it.key == key || it.key == option.key }
            if (index < 0) others + line
            else others.toMutableList().apply { add(index.coerceAtMost(size), line) }
        }
    }

    fun setQuantity(key: String, quantity: Double) {
        _lines.update { current ->
            current.mapNotNull { line ->
                when {
                    line.key != key -> line
                    quantity <= LooseQty.EPSILON -> null
                    else -> line.copy(quantity = line.option.normalize(quantity))
                }
            }
        }
    }

    fun increment(key: String) {
        _lines.update { current ->
            current.map { if (it.key == key) it.copy(quantity = it.option.normalize(it.quantity + it.option.step)) else it }
        }
    }

    /** Steps down; removes the line once it would drop below the option's minimum. */
    fun decrement(key: String) {
        _lines.update { current ->
            current.mapNotNull { line ->
                when {
                    line.key != key -> line
                    line.quantity - line.option.step + LooseQty.EPSILON >= line.option.minQty ->
                        line.copy(quantity = line.option.normalize(line.quantity - line.option.step))
                    else -> null
                }
            }
        }
    }

    fun remove(key: String) {
        _lines.update { current -> current.filterNot { it.key == key } }
    }

    fun linesFor(productId: String): List<Line> = _lines.value.filter { it.product.id == productId }

    /** Pack count for simple products; used by the one-tap stepper. */
    fun quantityOf(productId: String): Int =
        _lines.value.firstOrNull { it.product.id == productId && it.variant == null && !it.isLoose }?.quantity?.toInt() ?: 0

    /** Packs count individually; a loose line counts as one item. */
    val itemCount: Int get() = countItems(_lines.value)

    val subtotal: Double get() = _lines.value.sumOf { it.lineTotal }

    fun clear() {
        _lines.value = emptyList()
    }

    companion object {
        fun lineKey(productId: String, variantId: String?): String =
            if (variantId.isNullOrBlank()) productId else "$productId#$variantId"

        fun countItems(lines: List<Line>): Int = lines.sumOf { if (it.isLoose) 1 else it.quantity.toInt() }
    }
}
