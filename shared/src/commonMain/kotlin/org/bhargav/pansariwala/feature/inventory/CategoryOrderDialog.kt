package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.CategorySort
import org.bhargav.pansariwala.i18n.asString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.cat_order_by_name
import pansariwala.shared.generated.resources.cat_order_by_priority
import pansariwala.shared.generated.resources.cat_order_empty
import pansariwala.shared.generated.resources.cat_order_name_hint
import pansariwala.shared.generated.resources.cat_order_priority_hint
import pansariwala.shared.generated.resources.cat_order_priority_label
import pansariwala.shared.generated.resources.cat_order_product_count
import pansariwala.shared.generated.resources.cat_order_title

@Composable
fun CategoryOrderDialog(
    state: CategoryOrderUiState,
    onSortChange: (CategorySort) -> Unit,
    onPriorityChange: (categoryId: String, value: String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cat_order_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sort == CategorySort.PRIORITY,
                        onClick = { onSortChange(CategorySort.PRIORITY) },
                        label = { Text(stringResource(Res.string.cat_order_by_priority)) },
                    )
                    FilterChip(
                        selected = state.sort == CategorySort.NAME,
                        onClick = { onSortChange(CategorySort.NAME) },
                        label = { Text(stringResource(Res.string.cat_order_by_name)) },
                    )
                }
                Text(
                    stringResource(
                        if (state.sort == CategorySort.PRIORITY) Res.string.cat_order_priority_hint
                        else Res.string.cat_order_name_hint,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                if (!state.loading && state.items.isEmpty()) {
                    Text(stringResource(Res.string.cat_order_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name.ifBlank { item.id }, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(Res.string.cat_order_product_count, item.productCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.sort == CategorySort.PRIORITY) {
                                OutlinedTextField(
                                    value = state.priorities[item.id].orEmpty(),
                                    onValueChange = { onPriorityChange(item.id, it) },
                                    label = { Text(stringResource(Res.string.cat_order_priority_label)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(96.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.loading && !state.saving) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
