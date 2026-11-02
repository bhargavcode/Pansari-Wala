package org.bhargav.pansariwala.data.local

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
    val role = if (access.role == ShopRoles.EXECUTIVE) {
        val preset = StaffRolePreset.of(access.permissions)
        if (preset == StaffRolePreset.CUSTOM) getShopRoleTitle().ifBlank { preset.name } else preset.name
    } else {
        ShopRoles.ADMIN
    }
    return StaffStamp(getUserId(), getDisplayName(), role)
}

fun Order.stampedBy(stamp: StaffStamp?): Order =
    if (stamp == null) this else copy(createdById = stamp.id, createdByName = stamp.name, createdByRole = stamp.role)
