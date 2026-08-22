package org.bhargav.pansariwala.feature.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.bhargav.pansariwala.domain.model.Product

class CartStore {
    data class Line(val product: Product, val quantity: Double)

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

    fun add(product: Product) {
        bindShop(product.shopId)
        _lines.update { current ->
            val existing = current.firstOrNull { it.product.id == product.id }
            if (existing == null) current + Line(product, 1.0)
            else current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    fun increment(productId: String) {
        _lines.update { current ->
            current.map { if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    fun decrement(productId: String) {
        _lines.update { current ->
            current.mapNotNull { line ->
                when {
                    line.product.id != productId -> line
                    line.quantity > 1 -> line.copy(quantity = line.quantity - 1)
                    else -> null
                }
            }
        }
    }

    fun quantityOf(productId: String): Int =
        _lines.value.firstOrNull { it.product.id == productId }?.quantity?.toInt() ?: 0

    val itemCount: Int get() = _lines.value.sumOf { it.quantity.toInt() }

    val subtotal: Double get() = _lines.value.sumOf { it.product.sellingPrice * it.quantity }

    fun clear() {
        _lines.value = emptyList()
    }
}
