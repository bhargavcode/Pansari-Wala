package org.bhargav.pansariwala.domain.model

import kotlinx.serialization.Serializable

/** Dealer / bulk provider the shop re-orders stock from. */
@Serializable
data class StockSupplier(
    val id: String,
    val name: String,
    val phone: String,
)

/** Shop's saved suppliers plus the last supplier used for each product. */
@Serializable
data class SupplierBook(
    val suppliers: List<StockSupplier> = emptyList(),
    val productSupplier: Map<String, String> = emptyMap(),
)
