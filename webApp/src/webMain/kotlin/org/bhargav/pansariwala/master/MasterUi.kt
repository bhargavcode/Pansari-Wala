package org.bhargav.pansariwala.master

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlin.math.round
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.media.imageFetchUrl
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_actions
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_add_product_card
import pansariwala.shared.generated.resources.master_address
import pansariwala.shared.generated.resources.master_amount_group
import pansariwala.shared.generated.resources.master_cancel_order
import pansariwala.shared.generated.resources.master_card_shops
import pansariwala.shared.generated.resources.master_card_transactions
import pansariwala.shared.generated.resources.master_card_users
import pansariwala.shared.generated.resources.master_charges
import pansariwala.shared.generated.resources.master_close
import pansariwala.shared.generated.resources.master_contact
import pansariwala.shared.generated.resources.master_customer
import pansariwala.shared.generated.resources.master_customer_name
import pansariwala.shared.generated.resources.master_date_filter
import pansariwala.shared.generated.resources.master_desc_bold
import pansariwala.shared.generated.resources.master_desc_italic
import pansariwala.shared.generated.resources.master_desc_underline
import pansariwala.shared.generated.resources.master_description
import pansariwala.shared.generated.resources.master_filter_custom
import pansariwala.shared.generated.resources.master_filter_monthly
import pansariwala.shared.generated.resources.master_filter_today
import pansariwala.shared.generated.resources.master_filter_weekly
import pansariwala.shared.generated.resources.master_filter_yearly
import pansariwala.shared.generated.resources.master_filter_yesterday
import pansariwala.shared.generated.resources.master_item_details
import pansariwala.shared.generated.resources.master_new_shop
import pansariwala.shared.generated.resources.master_notifications
import pansariwala.shared.generated.resources.master_offers
import pansariwala.shared.generated.resources.master_order_id
import pansariwala.shared.generated.resources.master_paid
import pansariwala.shared.generated.resources.master_profile
import pansariwala.shared.generated.resources.master_refund
import pansariwala.shared.generated.resources.master_sales_overview
import pansariwala.shared.generated.resources.master_shop_details_col
import pansariwala.shared.generated.resources.master_shop_id
import pansariwala.shared.generated.resources.master_shop_name
import pansariwala.shared.generated.resources.master_sku
import pansariwala.shared.generated.resources.master_star
import pansariwala.shared.generated.resources.master_total
import pansariwala.shared.generated.resources.master_transaction_no
import pansariwala.shared.generated.resources.master_txn_trends
import pansariwala.shared.generated.resources.master_view_more

@Composable
fun LoginBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                ),
            ),
        ),
    )
}

@Composable
fun MasterTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    MasterHeaderSurface(modifier) {
        val content = LocalContentColor.current
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppConstants.MasterWeb.CONTENT_PADDING_DP.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MasterThemeMenu(tint = content)
                Box {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(Res.string.master_notifications), tint = content)
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                    )
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = stringResource(Res.string.master_profile), tint = content)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardTopCards(
    shopCount: Int,
    txnAmount: Double,
    userCount: Int,
    txnFilter: String,
    onTxnFilter: (String) -> Unit,
    onAddShop: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenTransactions: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MasterCard(
            modifier = Modifier.width(280.dp).clickable(onClick = onOpenShops),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(Res.string.master_card_shops), style = MaterialTheme.typography.titleSmall)
                }
                Text("$shopCount", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Button(onClick = onAddShop, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.master_new_shop))
                }
            }
        }
        DateFilterSummaryCard(
            title = stringResource(Res.string.master_card_transactions),
            value = formatInr(txnAmount),
            selected = txnFilter,
            onSelect = onTxnFilter,
            onClick = onOpenTransactions,
            modifier = Modifier.width(280.dp),
        )
        MasterCard(
            modifier = Modifier.width(220.dp).clickable(onClick = onOpenUsers),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.master_card_users), style = MaterialTheme.typography.titleSmall)
                Text("$userCount", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DateFilterSummaryCard(
    title: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        AppConstants.DateFilter.TODAY to Res.string.master_filter_today,
        AppConstants.DateFilter.YESTERDAY to Res.string.master_filter_yesterday,
        AppConstants.DateFilter.WEEKLY to Res.string.master_filter_weekly,
        AppConstants.DateFilter.MONTHLY to Res.string.master_filter_monthly,
        AppConstants.DateFilter.YEARLY to Res.string.master_filter_yearly,
        AppConstants.DateFilter.CUSTOM to Res.string.master_filter_custom,
    )
    MasterCard(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { open = true }) {
                    Text(options.firstOrNull { it.first == selected }?.let { stringResource(it.second) } ?: stringResource(Res.string.master_date_filter))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    options.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            onClick = { onSelect(id); open = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductPromoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.master_add_product_card),
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(AppConstants.MasterWeb.CARD_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SimpleBarChart(title: String, points: List<AdminChartPointDto>, modifier: Modifier = Modifier) {
    MasterSectionCard(title = title, modifier = modifier) {
        val max = points.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
        val color = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (points.isEmpty()) return@Canvas
            val gap = size.width / (points.size * 2)
            points.forEachIndexed { i, p ->
                val barW = gap * 0.75f
                val h = (p.value / max * (size.height - 24f)).toFloat().coerceAtLeast(4f)
                val x = gap + i * gap * 2
                drawRect(color, topLeft = Offset(x, size.height - h), size = Size(barW, h))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
fun SimpleLineChart(title: String, points: List<AdminChartPointDto>, modifier: Modifier = Modifier) {
    MasterSectionCard(title = title, modifier = modifier) {
        val max = points.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
        val line = MaterialTheme.colorScheme.primary
        val fill = line.copy(alpha = 0.15f)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1)
            val path = Path()
            val fillPath = Path()
            points.forEachIndexed { i, p ->
                val x = i * stepX
                val y = size.height - (p.value / max * (size.height - 20f)).toFloat()
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()
            drawPath(fillPath, fill)
            drawPath(path, line, style = Stroke(width = 3f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.take(6).forEach { Text(it.label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
fun ViewMoreButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(stringResource(Res.string.master_view_more)) }
}

@Composable
fun RatingText(rating: Double) {
    val rounded = round(rating * 10.0) / 10.0
    Text("$rounded ${stringResource(Res.string.master_star)}", fontWeight = FontWeight.Medium)
}

@Composable
fun ImageThumb(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        val target = url?.trim()?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        bitmap = runCatching {
            createPlatformHttpClient().get(imageFetchUrl(target)).readRawBytes().decodeToImageBitmap()
        }.getOrNull()
    }
    Box(
        modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DescriptionEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.master_description), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onValueChange("**$value**") }) { Text(stringResource(Res.string.master_desc_bold), fontWeight = FontWeight.Bold) }
            TextButton(onClick = { onValueChange("*$value*") }) { Text(stringResource(Res.string.master_desc_italic)) }
            TextButton(onClick = { onValueChange("_${value}_") }) { Text(stringResource(Res.string.master_desc_underline)) }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = { Text(stringResource(Res.string.master_description)) },
        )
    }
}

@Composable
fun NestedTxnTableHeader() {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderCell(stringResource(Res.string.master_order_id), 1.1f)
            HeaderCell(stringResource(Res.string.master_transaction_no), 1.2f)
            HeaderCell(stringResource(Res.string.master_item_details), 1.6f)
            HeaderCell(stringResource(Res.string.master_customer), 2.4f, center = true)
            HeaderCell(stringResource(Res.string.master_shop_details_col), 1.6f, center = true)
            HeaderCell(stringResource(Res.string.master_amount_group), 2.4f, center = true)
            HeaderCell(stringResource(Res.string.master_actions), 1.2f)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1.1f + 1.2f + 1.6f))
            SubHeaderCell(stringResource(Res.string.master_customer_name), 0.8f)
            SubHeaderCell(stringResource(Res.string.master_contact), 0.8f)
            SubHeaderCell(stringResource(Res.string.master_address), 0.8f)
            SubHeaderCell(stringResource(Res.string.master_shop_name), 1.0f)
            SubHeaderCell(stringResource(Res.string.master_shop_id), 0.6f)
            SubHeaderCell(stringResource(Res.string.master_offers), 0.6f)
            SubHeaderCell(stringResource(Res.string.master_charges), 0.6f)
            SubHeaderCell(stringResource(Res.string.master_total), 0.6f)
            SubHeaderCell(stringResource(Res.string.master_paid), 0.6f)
            Spacer(Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float, center: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowScope.SubHeaderCell(text: String, weight: Float) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun NestedTxnTableRow(
    txn: TxnDto,
    actions: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CellWeight(txn.orderId, 1.1f)
        CellWeight(txn.transactionNo, 1.2f)
        CellWeight(txn.itemsSummary, 1.6f)
        CellWeight(txn.customerName, 0.8f)
        CellWeight(txn.customerPhone, 0.8f)
        CellWeight(txn.customerAddress, 0.8f)
        CellWeight(txn.shopName, 1.0f)
        CellWeight(txn.shopId, 0.6f)
        CellWeight(formatInr(txn.offers), 0.6f)
        CellWeight(formatInr(txn.charges), 0.6f)
        CellWeight(formatInr(txn.total), 0.6f)
        CellWeight(formatInr(txn.paid), 0.6f)
        Row(Modifier.weight(1.2f), content = actions)
    }
}

@Composable
private fun RowScope.CellWeight(text: String, weight: Float) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ResponsiveColumns(
    modifier: Modifier = Modifier,
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
) {
    AdaptivePane(modifier) { widthClass ->
        if (widthClass == WindowWidthClass.Compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(content = left)
                Column(content = right)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), content = left)
                Column(Modifier.weight(1f), content = right)
            }
        }
    }
}

@Composable
fun OrderDetailDialog(
    txn: TxnDto?,
    onDismiss: () -> Unit,
    onRefund: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    if (txn == null) return
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(Res.string.master_order_id)}: #${txn.orderId}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${stringResource(Res.string.master_customer)}: ${txn.customerName}")
                Text(txn.customerPhone)
                Text(txn.customerAddress)
                Text("${stringResource(Res.string.master_offers)}: ${formatInr(txn.offers)}")
                Text("${stringResource(Res.string.master_charges)}: ${formatInr(txn.charges)}")
                Text("${stringResource(Res.string.master_total)}: ${formatInr(txn.total)}")
                Text("${stringResource(Res.string.master_paid)}: ${formatInr(txn.paid)}")
                txn.items.forEach { item ->
                    Text("• ${item.productName} x${item.quantity}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRefund(txn.orderId) }) { Text(stringResource(Res.string.master_refund)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onCancel(txn.orderId) }) { Text(stringResource(Res.string.master_cancel_order)) }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.master_close)) }
            }
        },
    )
}

@Composable
fun StatsGrid(cells: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cells.forEach { (label, value) ->
            Card(
                modifier = Modifier.width(140.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
