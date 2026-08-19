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

    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines.asStateFlow()

    fun bindShop(shopId: String) {
        if (_shopId.value != shopId) {
            _shopId.value = shopId
            _lines.value = emptyList()
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

    fun clear() {
        _lines.value = emptyList()
    }
}
