package org.bhargav.pansariwala.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Typed Nav3 destinations (custom data model → destination).
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Dashboard : AppRoute

    @Serializable
    data class AddEditInventory(val productId: String? = null) : AppRoute

    @Serializable
    data class InventoryList(val lowStockOnly: Boolean = false) : AppRoute

    @Serializable
    data class OrderEditor(val orderId: String? = null) : AppRoute
}

fun AppRoute.screenName(): String = when (this) {
    AppRoute.Splash -> "splash"
    AppRoute.Login -> "login"
    AppRoute.Dashboard -> "dashboard"
    is AppRoute.AddEditInventory -> "add_edit_inventory"
    is AppRoute.InventoryList -> "inventory_list"
    is AppRoute.OrderEditor -> "order_editor"
}
