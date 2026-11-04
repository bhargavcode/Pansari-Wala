package org.bhargav.pansariwala.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.domain.auth.ShopRoles
import org.bhargav.pansariwala.domain.auth.StaffRolePreset
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.report_profit_cost
import pansariwala.shared.generated.resources.report_profit_margin
import pansariwala.shared.generated.resources.report_profit_missing_cost
import pansariwala.shared.generated.resources.report_profit_title
import pansariwala.shared.generated.resources.report_staff_all
import pansariwala.shared.generated.resources.report_filter_role
import pansariwala.shared.generated.resources.report_filter_selected
import pansariwala.shared.generated.resources.report_filter_staff
import pansariwala.shared.generated.resources.report_role_all
import pansariwala.shared.generated.resources.report_staff_orders
import pansariwala.shared.generated.resources.report_staff_section
import pansariwala.shared.generated.resources.staff_role_admin
import pansariwala.shared.generated.resources.staff_role_cashier
import pansariwala.shared.generated.resources.staff_role_custom
import pansariwala.shared.generated.resources.staff_role_manager
import pansariwala.shared.generated.resources.staff_role_owner
import pansariwala.shared.generated.resources.staff_role_packer
import pansariwala.shared.generated.resources.staff_role_stock

private val StaffTint = Color(0xFF1565C0)
private val RoleTint = Color(0xFF6A1B9A)
private val ProfitGreen = Color(0xFF2E7D32)
private val LossRed = Color(0xFFC62828)

/** Label for a stored role key ([ShopRoles.ADMIN] or a [StaffRolePreset] name). */
fun staffRoleLabel(role: String?): StringResource? = when (role) {
    null -> null
    ShopRoles.ADMIN -> Res.string.staff_role_owner
    StaffRolePreset.CASHIER.name -> Res.string.staff_role_cashier
    StaffRolePreset.ORDER_PACKER.name -> Res.string.staff_role_packer
    StaffRolePreset.STOCK_KEEPER.name -> Res.string.staff_role_stock
    StaffRolePreset.MANAGER.name -> Res.string.staff_role_manager
    StaffRolePreset.ADMIN.name -> Res.string.staff_role_admin
    else -> Res.string.staff_role_custom
}

/** Display text for a stored role; shop-defined custom role titles are shown as entered. */
@Composable
fun staffRoleText(role: String?): String? {
    val res = staffRoleLabel(role) ?: return null
    val isKnown = role == ShopRoles.ADMIN || StaffRolePreset.entries.any { it.name == role }
    return if (isKnown || role.isNullOrBlank()) stringResource(res) else role
}

/** Staff + role multi-select dropdowns; the staff list only offers people in the ticked roles. */
@Composable
internal fun StaffRoleFilters(
    staff: List<StaffSales>,
    selectedStaff: Set<String>,
    selectedRoles: Set<String>,
    onToggleStaff: (String?) -> Unit,
    onToggleRole: (String?) -> Unit,
) {
    if (staff.isEmpty()) return
    val roles = staff.mapNotNull { it.role }.distinct()
    val staffOptions = if (selectedRoles.isEmpty()) staff else staff.filter { it.role in selectedRoles }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MultiSelectDropdown(
            label = stringResource(Res.string.report_filter_staff),
            icon = Icons.Default.Person,
            tint = StaffTint,
            allLabel = stringResource(Res.string.report_staff_all),
            options = staffOptions.map { s -> s.staffId to s.name },
            optionHint = { id -> staffOptions.firstOrNull { it.staffId == id }?.role?.let { staffRoleText(it) } },
            selected = selectedStaff,
            onToggle = onToggleStaff,
            modifier = Modifier.weight(1f),
        )
        if (roles.isNotEmpty()) {
            MultiSelectDropdown(
                label = stringResource(Res.string.report_filter_role),
                icon = Icons.Default.Badge,
                tint = RoleTint,
                allLabel = stringResource(Res.string.report_role_all),
                options = roles.map { it to (staffRoleText(it) ?: it) },
                optionHint = { null },
                selected = selectedRoles,
                onToggle = onToggleRole,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MultiSelectDropdown(
    label: String,
    icon: ImageVector,
    tint: Color,
    allLabel: String,
    options: List<Pair<String, String>>,
    optionHint: @Composable (String) -> String?,
    selected: Set<String>,
    onToggle: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = when {
        selected.isEmpty() -> allLabel
        selected.size == 1 -> options.firstOrNull { it.first in selected }?.second ?: allLabel
        else -> stringResource(Res.string.report_filter_selected, selected.size)
    }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = if (selected.isEmpty()) MaterialTheme.colorScheme.surfaceContainerHigh else tint.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Checkbox(checked = selected.isEmpty(), onCheckedChange = null) },
                onClick = { onToggle(null) },
            )
            HorizontalDivider()
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(name)
                            optionHint(id)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    leadingIcon = { Checkbox(checked = id in selected, onCheckedChange = null) },
                    onClick = { onToggle(id) },
                )
            }
        }
    }
}

@Composable
internal fun ProfitCard(profit: ProfitSummary) {
    val positive = profit.profit >= 0.0
    val accent = if (positive) ProfitGreen else LossRed
    Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.04f))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(36.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(stringResource(Res.string.report_profit_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(profit.profit.asMoney(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(
                stringResource(Res.string.report_profit_margin, profit.marginPercent.toInt()) + "  ·  " +
                    stringResource(Res.string.report_profit_cost, profit.cost.asMoney()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profit.linesWithoutCost > 0) {
                Text(
                    stringResource(Res.string.report_profit_missing_cost, profit.linesWithoutCost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun StaffSalesSection(
    allStaff: List<StaffSales>,
    selectedStaff: Set<String>,
    selectedRoles: Set<String>,
    onToggle: (String) -> Unit,
) {
    val staff = allStaff.filter { s ->
        (selectedStaff.isEmpty() || s.staffId in selectedStaff) && (selectedRoles.isEmpty() || s.role in selectedRoles)
    }
    if (staff.isEmpty()) return
    val top = staff.maxOf { it.revenue }.coerceAtLeast(1.0)
    SectionCard(title = stringResource(Res.string.report_staff_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            staff.forEach { s ->
                val isSelected = s.staffId in selectedStaff
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(s.staffId) }
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                            val role = staffRoleText(s.role)
                            Text(
                                listOfNotNull(role, stringResource(Res.string.report_staff_orders, s.orders)).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(s.revenue.asMoney(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { (s.revenue / top).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
