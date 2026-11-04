package org.bhargav.pansariwala.data.local

import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.auth.ShopRoles
import org.bhargav.pansariwala.domain.auth.StaffRolePreset
import org.bhargav.pansariwala.domain.model.Order

/**
 * Who is recording a transaction: login id, display name and role — [ShopRoles.ADMIN], a [StaffRolePreset]
 * name, or the shop's own title for a custom role.
 */
data class StaffStamp(val id: String?, val name: String?, val role: String?)

suspend fun AppPreferences.currentStaffStamp(): StaffStamp {
    val access = getShopAccess()
    val role = staffRoleKey(access.role, access.permissions) { getShopRoleTitle() }
    return StaffStamp(getUserId(), getDisplayName(), role)
}

/** Role key stamped on transactions for a login with [role] / [permissions]; see [StaffStamp]. */
inline fun staffRoleKey(role: String, permissions: Set<ShopPermission>, customTitle: () -> String): String =
    if (role == ShopRoles.EXECUTIVE) {
        val preset = StaffRolePreset.of(permissions)
        if (preset == StaffRolePreset.CUSTOM) customTitle().ifBlank { preset.name } else preset.name
    } else {
        ShopRoles.ADMIN
    }

fun Order.stampedBy(stamp: StaffStamp?): Order =
    if (stamp == null) this else copy(createdById = stamp.id, createdByName = stamp.name, createdByRole = stamp.role)
