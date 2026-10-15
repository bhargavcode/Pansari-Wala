package org.bhargav.pansariwala.domain.auth

/** Keep names in sync with the server `ShopPerm` constants. */
enum class ShopPermission {
    ADMIN,
    BILLING,
    ONLINE_ORDERS,
    INVENTORY,
    VIEW_REPORTS,
    PRINT_REPORTS,
    TOGGLE_FEATURES,
    SETTINGS,
    SHOP_PROFILE,
    MANAGE_STAFF,
    OFFERS,
    RATINGS,
    VISITORS,
    ;

    companion object {
        /** Grantable to executives; [ADMIN] is shown separately as "full admin access". */
        val grantable: List<ShopPermission> = entries - ADMIN

        fun parse(raw: Collection<String>): Set<ShopPermission> =
            raw.mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
    }
}

object ShopRoles {
    const val ADMIN: String = "SHOP"
    const val EXECUTIVE: String = "EXECUTIVE"
}

data class ShopAccess(
    val role: String,
    val permissions: Set<ShopPermission>,
) {
    val isAdmin: Boolean get() = role != ShopRoles.EXECUTIVE || ShopPermission.ADMIN in permissions

    fun can(permission: ShopPermission): Boolean = isAdmin || permission in permissions

    companion object {
        /** Legacy sessions and offline (seeded) logins keep full access. */
        val FULL = ShopAccess(ShopRoles.ADMIN, ShopPermission.entries.toSet())
    }
}
