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
    val autoRequest: SupplierAutoRequest = SupplierAutoRequest(),
    /** productId → epoch ms of the last automatic request, to avoid re-alerting every sale. */
    val lastAutoAt: Map<String, Long> = emptyMap(),
)

/** POS-controlled automatic restock requests raised when a sale drops a linked product to low stock. */
@Serializable
data class SupplierAutoRequest(
    val enabled: Boolean = false,
    val appNotify: Boolean = true,
    /** Chargeable WhatsApp Business delivery; stored now, sent once a paid provider is configured. */
    val whatsapp: Boolean = false,
)
