package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.StockSupplier
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.stock_req_add_supplier
import pansariwala.shared.generated.resources.stock_req_msg_greeting
import pansariwala.shared.generated.resources.stock_req_msg_intro
import pansariwala.shared.generated.resources.stock_req_msg_signoff
import pansariwala.shared.generated.resources.stock_req_send_sms
import pansariwala.shared.generated.resources.stock_req_send_whatsapp
import pansariwala.shared.generated.resources.stock_req_supplier_name
import pansariwala.shared.generated.resources.stock_req_supplier_phone
import pansariwala.shared.generated.resources.suppliers_empty
import pansariwala.shared.generated.resources.suppliers_filter_all
import pansariwala.shared.generated.resources.suppliers_filter_linked
import pansariwala.shared.generated.resources.suppliers_filter_low
import pansariwala.shared.generated.resources.suppliers_filter_out
import pansariwala.shared.generated.resources.suppliers_hint
import pansariwala.shared.generated.resources.suppliers_linked_count
import pansariwala.shared.generated.resources.suppliers_msg_line
import pansariwala.shared.generated.resources.suppliers_no_products
import pansariwala.shared.generated.resources.suppliers_pick_low
import pansariwala.shared.generated.resources.suppliers_selected
import pansariwala.shared.generated.resources.suppliers_stock
import pansariwala.shared.generated.resources.suppliers_title

/** Supplier list with their products, stock filters and a multi-product restock request. */
@Composable
fun SuppliersScreen(onBack: () -> Unit, viewModel: SuppliersViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val message = supplierRequestMessage(state)

    fun send(channel: StockRequestChannel) {
        val supplier = viewModel.confirmSend() ?: return
        runCatching { uriHandler.openUri(stockRequestUri(channel, supplier, message)) }
    }

    PansariScreen(
        title = stringResource(Res.string.suppliers_title),
        onBack = onBack,
        isLoading = state.loading,
        actions = {
            IconButton(onClick = { viewModel.showAddSupplier(true) }) {
                Icon(Icons.Filled.PersonAdd, contentDescription = stringResource(Res.string.stock_req_add_supplier))
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            SupplierStrip(state, viewModel)
            FilterStrip(state.filter, state.selected != null, viewModel::setFilter, viewModel::pickAllLow)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.suppliers.isNotEmpty()) {
                    item(key = "auto") { SupplierAutoRequestCard(state.autoRequest, viewModel::setAutoRequest) }
                }
                val rows = state.visibleProducts
                if (rows.isEmpty() && !state.loading) {
                    item(key = "empty") {
                        Text(
                            stringResource(Res.string.suppliers_no_products),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(rows, key = { it.id }) { product ->
                    SupplierProductRow(
                        product = product,
                        supplierName = state.productSupplier[product.id]
                            ?.let { id -> state.suppliers.firstOrNull { it.id == id }?.name },
                        qty = state.picked[product.id],
                        onToggle = { viewModel.togglePick(product) },
                        onQtyChange = { viewModel.setQty(product.id, it) },
                    )
                }
            }
            if (state.requestLines.isNotEmpty()) {
                SendBar(
                    count = state.requestLines.size,
                    enabled = state.selected != null,
                    onWhatsApp = { send(StockRequestChannel.WHATSAPP) },
                    onSms = { send(StockRequestChannel.SMS) },
                )
            }
        }
    }

    if (state.addingSupplier) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddSupplier(false) },
            icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
            title = { Text(stringResource(Res.string.stock_req_add_supplier)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.nameInput,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(Res.string.stock_req_supplier_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.phoneInput,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text(stringResource(Res.string.stock_req_supplier_phone)) },
                        prefix = { Text(AppConstants.DEFAULT_PHONE_COUNTRY_CODE + " ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { Button(onClick = viewModel::saveSupplier) { Text(stringResource(Res.string.action_save)) } },
            dismissButton = {
                TextButton(onClick = { viewModel.showAddSupplier(false) }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SupplierStrip(state: SuppliersUiState, viewModel: SuppliersViewModel) {
    if (state.suppliers.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.suppliers_empty), fontWeight = FontWeight.SemiBold)
                Text(stringResource(Res.string.suppliers_hint), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { viewModel.showAddSupplier(true) }) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.stock_req_add_supplier), Modifier.padding(start = 6.dp))
                }
            }
        }
        return
    }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.suppliers.forEach { supplier ->
            SupplierCard(
                supplier = supplier,
                linked = state.linkedCount(supplier.id),
                selected = supplier.id == state.selectedId,
                onSelect = { viewModel.selectSupplier(supplier.id) },
                onRemove = { viewModel.removeSupplier(supplier.id) },
            )
        }
    }
}

@Composable
private fun SupplierCard(supplier: StockSupplier, linked: Int, selected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ElevatedCard(modifier = Modifier.width(200.dp).clickable(onClick = onSelect)) {
        Surface(color = container) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = content)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(supplier.name, color = content, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(supplier.phone, color = content, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(Res.string.suppliers_linked_count, linked),
                        color = content,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (selected) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.action_remove), tint = content)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterStrip(
    filter: SupplierProductFilter,
    hasSupplier: Boolean,
    onFilter: (SupplierProductFilter) -> Unit,
    onPickLow: () -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SupplierProductFilter.entries
            .filter { hasSupplier || it != SupplierProductFilter.LINKED }
            .forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { onFilter(option) },
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    SupplierProductFilter.LINKED -> Res.string.suppliers_filter_linked
                                    SupplierProductFilter.OUT_OF_STOCK -> Res.string.suppliers_filter_out
                                    SupplierProductFilter.LOW_STOCK -> Res.string.suppliers_filter_low
                                    SupplierProductFilter.ALL -> Res.string.suppliers_filter_all
                                },
                            ),
                        )
                    },
                )
            }
        TextButton(onClick = onPickLow) {
            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.suppliers_pick_low), Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun SupplierProductRow(
    product: Product,
    supplierName: String?,
    qty: String?,
    onToggle: () -> Unit,
    onQtyChange: (String) -> Unit,
) {
    val unit = product.unit.localizedLabel()
    val stockColor = when {
        product.totalStock <= 0.0 -> MaterialTheme.colorScheme.error
        product.isLowStock -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = if (qty != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = qty != null, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(product.localizedName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(Res.string.suppliers_stock, product.totalStock.asQuantity(), unit),
                    color = stockColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                supplierName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (qty != null) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = onQtyChange,
                    singleLine = true,
                    suffix = { Text(unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp),
                )
            }
        }
    }
}

@Composable
private fun SendBar(count: Int, enabled: Boolean, onWhatsApp: () -> Unit, onSms: () -> Unit) {
    Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.suppliers_selected, count), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onWhatsApp, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.stock_req_send_whatsapp), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onSms, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.stock_req_send_sms), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun supplierRequestMessage(state: SuppliersUiState): String {
    val lines = buildList {
        add(stringResource(Res.string.stock_req_msg_greeting, state.selected?.name.orEmpty()).trim())
        add(stringResource(Res.string.stock_req_msg_intro))
        state.requestLines.forEach { (product, qty) ->
            val name = listOf(product.localizedName, product.brandName).filter { it.isNotBlank() }.joinToString(" · ")
            add(stringResource(Res.string.suppliers_msg_line, name, qty.asQuantity(), product.unit.localizedLabel()))
        }
        add(stringResource(Res.string.stock_req_msg_signoff, state.shopName).trim())
    }
    return lines.joinToString("\n")
}
