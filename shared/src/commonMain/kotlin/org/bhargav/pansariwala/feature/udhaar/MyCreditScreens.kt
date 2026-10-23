package org.bhargav.pansariwala.feature.udhaar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bhargav.pansariwala.api.MyCreditShopDto
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.printing.formatPrintDate
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.borrowed_empty
import pansariwala.shared.generated.resources.borrowed_repay_amount
import pansariwala.shared.generated.resources.borrowed_repay_confirm_title
import pansariwala.shared.generated.resources.borrowed_repay_no
import pansariwala.shared.generated.resources.borrowed_repay_pay
import pansariwala.shared.generated.resources.borrowed_repay_title
import pansariwala.shared.generated.resources.borrowed_repay_yes
import pansariwala.shared.generated.resources.borrowed_shop_due
import pansariwala.shared.generated.resources.borrowed_shops
import pansariwala.shared.generated.resources.borrowed_sub
import pansariwala.shared.generated.resources.borrowed_title
import pansariwala.shared.generated.resources.borrowed_view
import pansariwala.shared.generated.resources.udhaar_repayment
import pansariwala.shared.generated.resources.udhaar_total_credit
import pansariwala.shared.generated.resources.udhaar_total_repaid

private const val MESSAGE_VISIBLE_MS = 2_500L
private val borrowedGradient = listOf(Color(0xFFC62828), Color(0xFFEF6C00))

/** 4th card on the user app's More tab. */
@Composable
fun BorrowedCard(onClick: () -> Unit, viewModel: MyCreditViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val summary = state.summary
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(borrowedGradient)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${stringResource(Res.string.borrowed_title)} · ${stringResource(Res.string.borrowed_sub)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (summary?.totalOutstanding ?: 0.0).asMoney(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        if (state.loading) {
                            Spacer(Modifier.width(10.dp))
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.borrowed_shops, summary?.shops?.count { it.outstanding > 0 } ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.borrowed_view),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun MyCreditScreen(
    onBack: () -> Unit,
    onOpenShop: (String) -> Unit,
    viewModel: MyCreditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    PansariScreen(
        title = stringResource(Res.string.borrowed_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.summary == null,
        isRefreshing = state.loading && state.summary != null,
    ) {
        val summary = state.summary ?: return@PansariScreen
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
            ) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(borrowedGradient)).padding(16.dp)) {
                    Text(stringResource(Res.string.borrowed_sub), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(
                        summary.totalOutstanding.asMoney(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        stringResource(Res.string.borrowed_shops, summary.shops.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
            if (summary.shops.isEmpty()) {
                Text(
                    stringResource(Res.string.borrowed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            summary.shops.forEach { shop -> CreditShopCard(shop, onClick = { onOpenShop(shop.shopId) }) }
        }
    }
}

@Composable
private fun CreditShopCard(shop: MyCreditShopDto, onClick: (() -> Unit)?) {
    Card(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (!shop.shopImageUrl.isNullOrBlank()) {
                    NetworkImage(url = shop.shopImageUrl, contentDescription = shop.shopName, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    shop.shopName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (shop.shopAddress.isNotBlank()) {
                    Text(
                        shop.shopAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${stringResource(Res.string.udhaar_total_credit)} ${shop.totalCredit.asMoney()} · " +
                        "${stringResource(Res.string.udhaar_total_repaid)} ${shop.totalRepaid.asMoney()}" +
                        (shop.lastActivityAt.takeIf { it > 0 }?.let { " · ${formatPrintDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(Res.string.borrowed_shop_due, shop.outstanding.asMoney()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (shop.outstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCreditShopScreen(
    shopId: String,
    onBack: () -> Unit,
    viewModel: MyCreditShopViewModel = koinViewModel(key = "credit_shop_$shopId") { parametersOf(shopId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(state.launchUpi) {
        val uri = state.launchUpi ?: return@LaunchedEffect
        runCatching { uriHandler.openUri(uri) }
            .onSuccess { viewModel.onUpiLaunched() }
            .onFailure { viewModel.onUpiMissing() }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_VISIBLE_MS)
            viewModel.consumeMessage()
        }
    }
    val shop = state.detail?.shop
    PansariScreen(
        title = shop?.shopName ?: stringResource(Res.string.borrowed_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.detail == null,
        isRefreshing = state.loading && state.detail != null,
    ) {
        val detail = state.detail ?: return@PansariScreen
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = viewModel::openRepay,
                enabled = detail.shop.outstanding > 0.0 && state.pending == null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Default.Payments, null, Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.udhaar_repayment), style = MaterialTheme.typography.titleMedium)
            }
            CreditShopCard(detail.shop, onClick = null)
            CreditTxnsCard(
                transactions = detail.transactions,
                filter = state.filter,
                onFilter = viewModel::setFilter,
                showItems = true,
            )
            Spacer(Modifier.height(16.dp))
        }
        state.message?.let { msg ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Text(
                    msg.asString(),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    if (state.showRepay && shop != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeRepay,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(Res.string.borrowed_repay_title, shop.shopName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = { Text(stringResource(Res.string.borrowed_repay_amount, shop.outstanding.asMoney())) },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.repayError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = viewModel::startRepay,
                    enabled = !state.repayBusy && state.amount.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state.repayBusy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.borrowed_repay_pay))
                    }
                }
            }
        }
    }
    if (state.askOutcome) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.borrowed_repay_confirm_title)) },
            confirmButton = {
                Button(onClick = { viewModel.confirmOutcome(true) }) { Text(stringResource(Res.string.borrowed_repay_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmOutcome(false) }) { Text(stringResource(Res.string.borrowed_repay_no)) }
            },
        )
    }
}
