package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.dropdown_no_matches
import pansariwala.shared.generated.resources.dropdown_search_hint
import pansariwala.shared.generated.resources.master_all
import pansariwala.shared.generated.resources.master_category
import pansariwala.shared.generated.resources.master_shop_type
import pansariwala.shared.generated.resources.master_subcategory

data class DropdownOption(val id: String, val label: String)

/**
 * Text field that filters [options] as the user types. When [allLabel] is set, a leading
 * "All" entry selects `null`; otherwise the field always holds one of [options].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    options: List<DropdownOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: allLabel.orEmpty()
    val filtered = remember(options, query) {
        val q = query.trim()
        if (q.isEmpty()) options else options.filter { it.label.contains(q, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = {
            expanded = it
            query = ""
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) query else selectedLabel,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(if (expanded) selectedLabel.ifEmpty { stringResource(Res.string.dropdown_search_hint) } else "") },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            maxLines = 1,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 480.dp),
        ) {
            if (allLabel != null && query.isBlank()) {
                OptionItem(allLabel, selectedId == null) {
                    onSelect(null)
                    expanded = false
                }
            }
            filtered.forEach { option ->
                OptionItem(option.label, option.id == selectedId) {
                    onSelect(option.id)
                    expanded = false
                }
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(Res.string.dropdown_no_matches),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

/**
 * Checkbox dropdown; the field shows the picked labels comma-separated, or [allLabel] when
 * nothing is picked. The menu stays open while toggling so several can be picked at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    options: List<DropdownOption>,
    selectedIds: Set<String>,
    onChange: (Set<String>) -> Unit,
    allLabel: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val summary = options.filter { it.id in selectedIds }.joinToString(", ") { it.label }.ifEmpty { allLabel }
    val filtered = remember(options, query) {
        val q = query.trim()
        if (q.isEmpty()) options else options.filter { it.label.contains(q, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            query = ""
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) query else summary,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(if (expanded) summary else "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 480.dp),
        ) {
            if (query.isBlank()) {
                CheckItem(allLabel, selectedIds.isEmpty()) { onChange(emptySet()) }
            }
            filtered.forEach { option ->
                CheckItem(option.label, option.id in selectedIds) {
                    onChange(if (option.id in selectedIds) selectedIds - option.id else selectedIds + option.id)
                }
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(Res.string.dropdown_no_matches),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}

/**
 * Shop type → category → subcategory pickers. Picking a parent clears its children; the
 * subcategory picker only shows when [subcategories] is non-empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogCascadeFilter(
    shopTypes: List<DropdownOption>,
    shopTypeId: String?,
    onShopType: (String?) -> Unit,
    categories: List<DropdownOption>,
    categoryId: String?,
    onCategory: (String?) -> Unit,
    subcategories: List<DropdownOption>,
    subcategoryId: String?,
    onSubcategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
    fieldMinWidth: Dp = 220.dp,
) {
    val all = stringResource(Res.string.master_all)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val field = Modifier.weight(1f).widthIn(min = fieldMinWidth)
        SearchableDropdown(
            label = stringResource(Res.string.master_shop_type),
            options = shopTypes,
            selectedId = shopTypeId,
            onSelect = onShopType,
            allLabel = all,
            leadingIcon = Icons.Default.Storefront,
            modifier = field,
        )
        SearchableDropdown(
            label = stringResource(Res.string.master_category),
            options = categories,
            selectedId = categoryId,
            onSelect = onCategory,
            allLabel = all,
            leadingIcon = Icons.Default.Category,
            modifier = field,
        )
        if (subcategories.isNotEmpty()) {
            SearchableDropdown(
                label = stringResource(Res.string.master_subcategory),
                options = subcategories,
                selectedId = subcategoryId,
                onSelect = onSubcategory,
                allLabel = all,
                leadingIcon = Icons.Default.AccountTree,
                modifier = field,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}
