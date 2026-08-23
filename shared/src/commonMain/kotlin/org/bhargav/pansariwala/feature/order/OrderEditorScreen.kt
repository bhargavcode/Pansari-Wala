package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariSearchTopBar
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.cart_count
import pansariwala.shared.generated.resources.cart_empty_hint
import pansariwala.shared.generated.resources.create_order
import pansariwala.shared.generated.resources.customer_name
import pansariwala.shared.generated.resources.edit_order
import pansariwala.shared.generated.resources.in_stock
import pansariwala.shared.generated.resources.save_order
import pansariwala.shared.generated.resources.search_products
import pansariwala.shared.generated.resources.total_label
import pansariwala.shared.generated.resources.update_order
import pansariwala.shared.generated.resources.voice_hint_example

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
    PansariScreen(
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
        AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        if (widthClass == WindowWidthClass.Expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    OrderHeader(state.isEditing, onBack)
                    CustomerField(state.customerName, viewModel::onCustomerNameChange)
                    CartSection(state, viewModel)
                }
                Column(modifier = Modifier.weight(1f)) {
                    SearchOrSuggestedItems(state, viewModel, showHeader = false)
                }
            }
        } else {
            SearchOrSuggestedItems(state, viewModel, showHeader = true, onBack = onBack)
        }
        }
    }
}

@Composable
private fun OrderHeader(isEditing: Boolean, onBack: () -> Unit) {
    PansariTopBar(
        title = stringResource(if (isEditing) Res.string.edit_order else Res.string.create_order),
        onBack = onBack,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

@Composable
private fun CustomerField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(Res.string.customer_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.CartSection(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    Text(
        text = stringResource(Res.string.cart_count, state.itemCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
    HorizontalDivider()

    if (state.cart.isEmpty()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.cart_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes up remaining available vertical space
        ) {
            items(state.cart, key = { it.product.id }) { line ->
                CartLineRow(line, viewModel)
            }
        }
    }

    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.total_label, state.total.asMoney()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Button(onClick = viewModel::save) {
            Text(
                stringResource(
                    if (state.isEditing) Res.string.update_order else Res.string.save_order,
                ),
            )
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, viewModel: OrderEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(line.product.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${line.product.sellingPrice.asMoney()} · ${line.lineTotal.asMoney()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = {
            viewModel.changeQuantity(line.product.id, -1.0)
        }) { Text("−") }
        Text(
            text = "${line.quantity.asQuantity()} ${line.product.unit.localizedLabel()}",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = {
            viewModel.changeQuantity(line.product.id, 1.0)
        }) { Text("+") }
        TextButton(onClick = { viewModel.removeLine(line.product.id) }) {
            Text(stringResource(Res.string.action_remove))
        }
    }
}

@Composable
private fun SearchOrSuggestedItems(
    state: OrderEditorUiState,
    viewModel: OrderEditorViewModel,
    showHeader: Boolean,
    onBack: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showHeader) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OrderHeader(state.isEditing, onBack)
                CustomerField(state.customerName, viewModel::onCustomerNameChange)
                CartSection(state, viewModel)
            }
        }

        PansariSearchTopBar(
            title = stringResource(if (state.isEditing) Res.string.edit_order else Res.string.create_order),
            searchQuery = state.searchQuery,
            searchLabel = stringResource(Res.string.search_products),
            onSearchChange = viewModel::onSearchChange,
            onBack = if (showHeader) null else onBack,
            trailingSearchContent = {
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
                text = stringResource(Res.string.voice_hint_example),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(state.filteredCatalog, key = { it.id }) { product ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(
                                Res.string.in_stock,
                                product.sellingPrice.asMoney(),
                                product.stockQty.asQuantity(),
                                product.unit.localizedLabel(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { viewModel.addProduct(product) }) {
                        Text(stringResource(Res.string.action_add))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
