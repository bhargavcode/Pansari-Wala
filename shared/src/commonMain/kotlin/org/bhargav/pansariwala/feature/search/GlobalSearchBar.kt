package org.bhargav.pansariwala.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.feature.reports.staffRoleText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.udhaar_list_title
import pansariwala.shared.generated.resources.gsearch_clear
import pansariwala.shared.generated.resources.gsearch_hint
import pansariwala.shared.generated.resources.gsearch_listening
import pansariwala.shared.generated.resources.gsearch_mic
import pansariwala.shared.generated.resources.gsearch_mode_commands
import pansariwala.shared.generated.resources.gsearch_mode_orders
import pansariwala.shared.generated.resources.gsearch_no_results
import pansariwala.shared.generated.resources.gsearch_order_line
import pansariwala.shared.generated.resources.gsearch_replay
import pansariwala.shared.generated.resources.gsearch_stock_line
import pansariwala.shared.generated.resources.inventory_title
import pansariwala.shared.generated.resources.low_stock_items
import pansariwala.shared.generated.resources.create_order
import pansariwala.shared.generated.resources.online_orders_title
import pansariwala.shared.generated.resources.orders_workspace_title
import pansariwala.shared.generated.resources.printer_title
import pansariwala.shared.generated.resources.profile_title
import pansariwala.shared.generated.resources.reports_title
import pansariwala.shared.generated.resources.settings_title
import pansariwala.shared.generated.resources.suppliers_title

val SearchCommand.labelRes: StringResource
    get() = when (this) {
        SearchCommand.NEW_SALE -> Res.string.create_order
        SearchCommand.ORDERS -> Res.string.orders_workspace_title
        SearchCommand.ONLINE_ORDERS -> Res.string.online_orders_title
        SearchCommand.INVENTORY -> Res.string.inventory_title
        SearchCommand.LOW_STOCK -> Res.string.low_stock_items
        SearchCommand.SUPPLIERS -> Res.string.suppliers_title
        SearchCommand.REPORTS -> Res.string.reports_title
        SearchCommand.UDHAAR -> Res.string.udhaar_list_title
        SearchCommand.PRINTER -> Res.string.printer_title
        SearchCommand.SETTINGS -> Res.string.settings_title
        SearchCommand.PROFILE -> Res.string.profile_title
    }

private val SearchCommand.icon: ImageVector
    get() = when (this) {
        SearchCommand.NEW_SALE -> Icons.Default.AddShoppingCart
        SearchCommand.ORDERS -> Icons.AutoMirrored.Filled.ReceiptLong
        SearchCommand.ONLINE_ORDERS -> Icons.Default.ShoppingBag
        SearchCommand.INVENTORY -> Icons.Default.Inventory2
        SearchCommand.LOW_STOCK -> Icons.Default.WarningAmber
        SearchCommand.SUPPLIERS -> Icons.Default.LocalShipping
        SearchCommand.REPORTS -> Icons.Default.BarChart
        SearchCommand.UDHAAR -> Icons.Default.AccountBalanceWallet
        SearchCommand.PRINTER -> Icons.Default.Print
        SearchCommand.SETTINGS -> Icons.Default.Settings
        SearchCommand.PROFILE -> Icons.Default.Storefront
    }

private val CommandTint = Color(0xFF6A1B9A)
private val ProductTint = Color(0xFF2E7D32)
private val OrderTint = Color(0xFFEF6C00)

/**
 * POS-wide search: plain text = products, `/` = screens, `@` = recent bills. The mic listens once,
 * answers stock / price questions or opens the spoken screen, and reads the answer aloud.
 */
@Composable
fun GlobalSearchBar(
    onOpen: (SearchHit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GlobalSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val open: (SearchHit) -> Unit = { hit ->
        viewModel.clear()
        onOpen(hit)
    }
    LaunchedEffect(viewModel) { viewModel.commands.collect(onOpen) }
    RequestMicrophonePermission(
        trigger = state.requestMicPermission,
        onConsumed = viewModel::consumeMicPermissionRequest,
        onResult = viewModel::onMicPermissionResult,
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = {
                Text(
                    stringResource(if (state.listening) Res.string.gsearch_listening else Res.string.gsearch_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear(); focus.requestFocus() }) {
                            Icon(Icons.Default.Close, stringResource(Res.string.gsearch_clear))
                        }
                    }
                    if (state.voiceAvailable) {
                        IconButton(onClick = viewModel::onMicClick) {
                            Icon(
                                if (state.listening) Icons.Default.Stop else Icons.Default.Mic,
                                stringResource(Res.string.gsearch_mic),
                                tint = if (state.listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        state.answer?.let { AnswerCard(it, viewModel::replayAnswer) }
        AnimatedVisibility(visible = state.query.isNotEmpty() || state.mode != SearchMode.PRODUCTS) {
            ResultsCard(state, onOpen = open)
        }
    }
}

@Composable
private fun AnswerCard(answer: String, onReplay: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(answer, Modifier.weight(1f), color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onReplay) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(Res.string.gsearch_replay), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun ResultsCard(state: GlobalSearchUiState, onOpen: (SearchHit) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (state.mode) {
            SearchMode.COMMANDS -> ModeHeader(Res.string.gsearch_mode_commands, CommandTint)
            SearchMode.ORDERS -> ModeHeader(Res.string.gsearch_mode_orders, OrderTint)
            SearchMode.PRODUCTS -> Unit
        }
        if (state.hits.isEmpty()) {
            Text(
                stringResource(Res.string.gsearch_no_results),
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Card
        }
        LazyColumn(Modifier.heightIn(max = RESULTS_MAX_HEIGHT.dp)) {
            items(state.hits, key = { it.key() }) { hit ->
                HitRow(hit, state.languageCode) { onOpen(hit) }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ModeHeader(label: StringResource, tint: Color) {
    Text(
        stringResource(label),
        Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = tint,
    )
}

@Composable
private fun HitRow(hit: SearchHit, languageCode: String, onClick: () -> Unit) {
    val (icon, tint, title, subtitle) = when (hit) {
        is SearchHit.Command -> HitText(hit.command.icon, CommandTint, stringResource(hit.command.labelRes), null)
        is SearchHit.ProductHit -> HitText(
            Icons.Default.Inventory2,
            ProductTint,
            hit.product.displayName(languageCode),
            stringResource(
                Res.string.gsearch_stock_line,
                hit.product.totalStock.asQuantity(),
                hit.product.unit.localizedLabel(),
                hit.product.salePrice().asMoney(),
            ),
        )
        is SearchHit.OrderHit -> HitText(
            Icons.AutoMirrored.Filled.ReceiptLong,
            OrderTint,
            hit.order.customerName?.takeIf { it.isNotBlank() } ?: "#${hit.order.id.takeLast(ORDER_ID_TAIL)}",
            buildString {
                append(stringResource(Res.string.gsearch_order_line, hit.order.itemCount, hit.order.totalValue.asMoney()))
                hit.order.createdByName?.takeIf { it.isNotBlank() }?.let { name ->
                    append(" · ").append(name)
                    staffRoleText(hit.order.createdByRole)?.let { append(" (").append(it).append(")") }
                }
            },
        )
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(color = tint.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.padding(8.dp).size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

private data class HitText(val icon: ImageVector, val tint: Color, val title: String, val subtitle: String?)

private fun SearchHit.key(): String = when (this) {
    is SearchHit.Command -> "c_${command.name}"
    is SearchHit.ProductHit -> "p_${product.id}"
    is SearchHit.OrderHit -> "o_${order.id}"
}

private const val RESULTS_MAX_HEIGHT = 360
private const val ORDER_ID_TAIL = 6
