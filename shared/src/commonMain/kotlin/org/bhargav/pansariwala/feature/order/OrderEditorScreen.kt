package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OrderEditorScreen(
    orderId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OrderEditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) { viewModel.load(orderId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    RequestMicrophonePermission(
        trigger = state.requestMicPermission,
        onConsumed = viewModel::consumeMicPermissionRequest,
        onResult = viewModel::onMicPermissionResult,
    )
    AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        if (widthClass == WindowWidthClass.Expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.isEditing) "Edit order" else "Create order",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                    OutlinedTextField(
                        value = state.customerName,
                        onValueChange = viewModel::onCustomerNameChange,
                        label = { Text("Customer name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Cart (${state.itemCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                    if (state.cart.isEmpty()) {
                        Text(
                            text = "Add products by search or voice mic below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column {
                            state.cart.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            line.product.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${line.product.sellingPrice.asMoney()} · ${line.lineTotal.asMoney()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = {
                                        viewModel.changeQuantity(
                                            line.product.id,
                                            -1.0
                                        )
                                    }) { Text("−") }
                                    Text(
                                        text = "${line.quantity.asQuantity()} ${line.product.unit.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    TextButton(onClick = {
                                        viewModel.changeQuantity(
                                            line.product.id,
                                            1.0
                                        )
                                    }) { Text("+") }
                                    TextButton(onClick = { viewModel.removeLine(line.product.id) }) {
                                        Text(
                                            "Remove"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total: ${state.total.asMoney()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Button(onClick = viewModel::save) {
                            Text(if (state.isEditing) "Update order" else "Save order")
                        }
                    }
                    HorizontalDivider()
                }
                SearchOrSuggestedItems(state, onBack, viewModel)
            }
        } else {
            SearchOrSuggestedItems(state, onBack, viewModel)
        }
    }
}

@Composable
private fun SearchOrSuggestedItems(
    state: OrderEditorUiState,
    onBack: () -> Unit,
    viewModel: OrderEditorViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.isEditing) "Edit order" else "Create order",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onBack) { Text("Back") }
        }
        OutlinedTextField(
            value = state.customerName,
            onValueChange = viewModel::onCustomerNameChange,
            label = { Text("Customer name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Cart (${state.itemCount})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        if (state.cart.isEmpty()) {
            Text(
                text = "Add products by search or voice mic below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                state.cart.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                line.product.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${line.product.sellingPrice.asMoney()} · ${line.lineTotal.asMoney()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = {
                            viewModel.changeQuantity(
                                line.product.id,
                                -1.0
                            )
                        }) { Text("−") }
                        Text(
                            text = "${line.quantity.asQuantity()} ${line.product.unit.label}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            viewModel.changeQuantity(
                                line.product.id,
                                1.0
                            )
                        }) { Text("+") }
                        TextButton(onClick = { viewModel.removeLine(line.product.id) }) {
                            Text(
                                "Remove"
                            )
                        }
                    }
                }
            }
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total: ${state.total.asMoney()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = viewModel::save) {
                Text(if (state.isEditing) "Update order" else "Save order")
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchChange,
            label = { Text("Search products") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            trailingIcon = {
                ListeningMicButton(
                    isListening = state.isListening,
                    onClick = viewModel::onMicClick,
                    modifier = Modifier.size(40.dp),
                )
            },
        )

        if (state.isListening || state.partialTranscript.isNotBlank()) {
            VoiceSearchControls(
                isListening = state.isListening,
                partialTranscript = state.partialTranscript,
                onMicClick = viewModel::onMicClick,
                onCancel = viewModel::cancelListening,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                showMicButton = false,
            )
        } else {
            Text(
                text = "Mic se bolein: \"1 kilo chini, 2 kilo tuar daal\"",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.filteredCatalog, key = { it.id }) { product ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${product.sellingPrice.asMoney()} · ${product.stockQty.asQuantity()} ${product.unit.label} in stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { viewModel.addProduct(product) }) { Text("Add") }
                }
                HorizontalDivider()
            }
        }
    }
}
