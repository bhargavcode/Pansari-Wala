package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ktor.http.encodeURLParameter
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.StockSupplier
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.platform.e164Phone
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.inv_qty_field
import pansariwala.shared.generated.resources.stock_req_add_supplier
import pansariwala.shared.generated.resources.stock_req_current
import pansariwala.shared.generated.resources.stock_req_msg_barcode
import pansariwala.shared.generated.resources.stock_req_msg_brand
import pansariwala.shared.generated.resources.stock_req_msg_current
import pansariwala.shared.generated.resources.stock_req_msg_greeting
import pansariwala.shared.generated.resources.stock_req_msg_intro
import pansariwala.shared.generated.resources.stock_req_msg_note
import pansariwala.shared.generated.resources.stock_req_msg_product
import pansariwala.shared.generated.resources.stock_req_msg_qty
import pansariwala.shared.generated.resources.stock_req_msg_signoff
import pansariwala.shared.generated.resources.stock_req_note
import pansariwala.shared.generated.resources.stock_req_preview
import pansariwala.shared.generated.resources.stock_req_send_sms
import pansariwala.shared.generated.resources.stock_req_send_whatsapp
import pansariwala.shared.generated.resources.stock_req_supplier_name
import pansariwala.shared.generated.resources.stock_req_supplier_phone
import pansariwala.shared.generated.resources.stock_req_suppliers
import pansariwala.shared.generated.resources.stock_req_title

/** Pick or add a dealer and send them a restock request for [product] over WhatsApp or SMS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockRequestSheet(
    product: Product,
    onDismiss: () -> Unit,
    viewModel: StockRequestViewModel = koinViewModel(),
) {
    LaunchedEffect(product.id) { viewModel.open(product) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val unit = product.unit.localizedLabel()
    val message = stockRequestMessage(state, product, unit)

    fun send(channel: StockRequestChannel) {
        val supplier = viewModel.confirmSend() ?: return
        runCatching { uriHandler.openUri(stockRequestUri(channel, supplier, message)) }
        onDismiss()
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.stock_req_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.stock_req_current, product.stockQty.asQuantity(), unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.stock_req_suppliers),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (!state.addingSupplier) {
                    TextButton(onClick = { viewModel.showAddSupplier(true) }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(Res.string.stock_req_add_supplier), Modifier.padding(start = 6.dp))
                    }
                }
            }
            state.suppliers.forEach { supplier ->
                SupplierRow(
                    supplier = supplier,
                    selected = supplier.id == state.selectedId,
                    onSelect = { viewModel.selectSupplier(supplier.id) },
                    onRemove = { viewModel.removeSupplier(supplier.id) },
                )
            }
            if (state.addingSupplier) {
                AddSupplierForm(
                    name = state.nameInput,
                    phone = state.phoneInput,
                    canCancel = state.suppliers.isNotEmpty(),
                    onNameChange = viewModel::onNameChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onSave = viewModel::saveSupplier,
                    onCancel = { viewModel.showAddSupplier(false) },
                )
            }

            OutlinedTextField(
                value = state.qtyInput,
                onValueChange = viewModel::onQtyChange,
                label = { Text(stringResource(Res.string.inv_qty_field)) },
                suffix = { Text(unit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(Res.string.stock_req_note)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(Res.string.stock_req_preview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }

            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            val canSend = state.selected != null && !state.addingSupplier
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { send(StockRequestChannel.WHATSAPP) },
                    enabled = canSend,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.stock_req_send_whatsapp), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = { send(StockRequestChannel.SMS) },
                    enabled = canSend,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.stock_req_send_sms), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SupplierRow(supplier: StockSupplier, selected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.weight(1f)) {
            Text(supplier.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                supplier.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.action_remove))
        }
    }
}

@Composable
private fun AddSupplierForm(
    name: String,
    phone: String,
    canCancel: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.stock_req_supplier_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(Res.string.stock_req_supplier_phone)) },
            prefix = { Text(AppConstants.DEFAULT_PHONE_COUNTRY_CODE + " ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            if (canCancel) {
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) }
            }
            Button(onClick = onSave) { Text(stringResource(Res.string.action_save)) }
        }
    }
}

@Composable
private fun stockRequestMessage(state: StockRequestUiState, product: Product, unit: String): String {
    val lines = buildList {
        add(stringResource(Res.string.stock_req_msg_greeting, state.selected?.name.orEmpty()).trim())
        add(stringResource(Res.string.stock_req_msg_intro))
        add(stringResource(Res.string.stock_req_msg_product, product.name))
        if (product.brandName.isNotBlank()) add(stringResource(Res.string.stock_req_msg_brand, product.brandName))
        product.barcode?.takeIf { it.isNotBlank() }?.let { add(stringResource(Res.string.stock_req_msg_barcode, it)) }
        state.qty?.let { add(stringResource(Res.string.stock_req_msg_qty, it.asQuantity(), unit)) }
        add(stringResource(Res.string.stock_req_msg_current, product.stockQty.asQuantity(), unit))
        state.note.trim().takeIf { it.isNotEmpty() }?.let { add(stringResource(Res.string.stock_req_msg_note, it)) }
        add(stringResource(Res.string.stock_req_msg_signoff, state.shopName).trim())
    }
    return lines.joinToString("\n")
}

private fun stockRequestUri(channel: StockRequestChannel, supplier: StockSupplier, message: String): String {
    val phone = e164Phone(supplier.phone)
    val body = message.encodeURLParameter()
    return when (channel) {
        StockRequestChannel.WHATSAPP -> AppConstants.WHATSAPP_CHAT_URL + phone.removePrefix("+") + "?text=" + body
        // "?&body=" is understood by both Android and iOS messaging apps.
        StockRequestChannel.SMS -> "sms:$phone?&body=$body"
    }
}
