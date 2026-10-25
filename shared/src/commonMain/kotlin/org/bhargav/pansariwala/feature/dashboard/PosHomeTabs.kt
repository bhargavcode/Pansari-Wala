package org.bhargav.pansariwala.feature.dashboard

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import pansariwala.shared.generated.resources.pwd_change_subtitle
import pansariwala.shared.generated.resources.pwd_change_title
import androidx.compose.material.icons.filled.Lock
import pansariwala.shared.generated.resources.app_settings_subtitle
import pansariwala.shared.generated.resources.app_settings_title
import pansariwala.shared.generated.resources.pos_settings_subtitle
import pansariwala.shared.generated.resources.pos_settings_title
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Tune
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.StockLevel
import org.bhargav.pansariwala.feature.inventory.color
import org.bhargav.pansariwala.feature.inventory.label
import org.bhargav.pansariwala.feature.inventory.stockFraction
import org.bhargav.pansariwala.feature.order.NewOrdersBadge
import org.bhargav.pansariwala.feature.order.rememberNewOnlineOrderCount
import org.bhargav.pansariwala.feature.reports.InventoryPieChart
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.feature.catalog.productCategoryLabel
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_logout
import pansariwala.shared.generated.resources.action_settings
import pansariwala.shared.generated.resources.add_update_inventory
import pansariwala.shared.generated.resources.all_inventory
import pansariwala.shared.generated.resources.all_well_stocked
import pansariwala.shared.generated.resources.inv_tab_category_value
import pansariwala.shared.generated.resources.inv_tab_health
import pansariwala.shared.generated.resources.inv_tab_health_sub
import pansariwala.shared.generated.resources.inv_tab_low
import pansariwala.shared.generated.resources.inv_tab_master
import pansariwala.shared.generated.resources.inv_tab_out
import pansariwala.shared.generated.resources.inv_tab_products
import pansariwala.shared.generated.resources.inv_tab_title
import pansariwala.shared.generated.resources.inv_tab_top_value
import pansariwala.shared.generated.resources.inv_tab_value
import pansariwala.shared.generated.resources.low_stock_count
import pansariwala.shared.generated.resources.online_orders_title
import pansariwala.shared.generated.resources.orders_workspace_title
import pansariwala.shared.generated.resources.pos_more_borrowers_cta
import pansariwala.shared.generated.resources.bin_more_subtitle
import pansariwala.shared.generated.resources.bin_title
import pansariwala.shared.generated.resources.pos_more_borrowers_sub
import pansariwala.shared.generated.resources.pos_more_borrowers_title
import pansariwala.shared.generated.resources.pos_more_profile_sub
import pansariwala.shared.generated.resources.pos_more_sales_cta
import pansariwala.shared.generated.resources.pos_more_sales_title
import pansariwala.shared.generated.resources.pos_more_shortcuts
import pansariwala.shared.generated.resources.printer_title
import pansariwala.shared.generated.resources.sales_orders
import pansariwala.shared.generated.resources.shopkeeper_fallback
import pansariwala.shared.generated.resources.show_full_low_stock
import pansariwala.shared.generated.resources.stock_by_category
import pansariwala.shared.generated.resources.stock_left
import pansariwala.shared.generated.resources.sub_title
import pansariwala.shared.generated.resources.udhaar_total_due

enum class PosTab { POS, INVENTORY, MORE }

private const val CHART_ANIM_MS = 700

// ---------- Inventory tab ----------

@Composable
fun PosInventoryTab(
    data: DashboardData,
    onAddOrUpdateInventory: () -> Unit,
    onOpenMasterCatalog: () -> Unit,
    onShowLowStockList: () -> Unit,
    onShowFullInventory: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    val outCount = data.stockHealth[StockLevel.OUT_OF_STOCK] ?: 0
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            GradientCard(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
            ) { onColor ->
                Text(stringResource(Res.string.inv_tab_title), style = MaterialTheme.typography.labelLarge, color = onColor)
                Text(
                    data.totalInventoryValue.asMoney(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = onColor,
                )
                Text(
                    "${data.totalProducts} ${stringResource(Res.string.inv_tab_products)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = onColor.copy(alpha = 0.9f),
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddOrUpdateInventory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onColor,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Text(stringResource(Res.string.add_update_inventory), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenMasterCatalog,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = onColor),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.LibraryAdd, null, Modifier.size(18.dp))
                        Text(stringResource(Res.string.inv_tab_master), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(Res.string.inv_tab_products),
                    value = data.totalProducts.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(Res.string.inv_tab_low),
                    value = data.lowStockTotalCount.toString(),
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                StatTile(
                    label = stringResource(Res.string.inv_tab_out),
                    value = outCount.toString(),
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        item {
            SectionCard(
                title = stringResource(Res.string.inv_tab_health),
                subtitle = stringResource(Res.string.inv_tab_health_sub),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onShowFullInventory),
            ) {
                StockHealthDonut(data.stockHealth, data.totalProducts)
            }
        }
        item {
            SectionCard(title = stringResource(Res.string.inv_tab_category_value), modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InventoryPieChart(data.categoryBreakdown, Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Text(
                        stringResource(Res.string.stock_by_category),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CategoryBarChart(data = data.categoryBreakdown, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (data.topValueProducts.isNotEmpty()) {
            item {
                SectionCard(title = stringResource(Res.string.inv_tab_top_value), modifier = Modifier.fillMaxWidth()) {
                    TopValueBars(data.topValueProducts, onOpenProduct)
                }
            }
        }
        item {
            SectionCard(
                title = stringResource(Res.string.low_stock_count, data.lowStockTotalCount),
                modifier = Modifier.fillMaxWidth(),
                action = { TextButton(onClick = onShowFullInventory) { Text(stringResource(Res.string.all_inventory)) } },
            ) {
                if (data.lowStockItems.isEmpty()) {
                    Text(
                        stringResource(Res.string.all_well_stocked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.lowStockItems.forEach { product -> LowStockGaugeRow(product, onOpenProduct) }
                        TextButton(onClick = onShowLowStockList, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(Res.string.show_full_low_stock))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun rememberGrow(key: Any?): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(CHART_ANIM_MS))
    }
    return progress.value
}

@Composable
private fun StockHealthDonut(health: Map<StockLevel, Int>, total: Int) {
    val levels = StockLevel.entries.reversed()
    val progress = rememberGrow(health)
    val colors = levels.map { it.color() }
    val sum = health.values.sum().coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.widthIn(max = 150.dp).weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.18f
                val inset = stroke / 2f
                var start = -90f
                levels.forEachIndexed { i, level ->
                    val sweep = (health[level] ?: 0) * 360f / sum * progress
                    if (sweep > 0f) {
                        drawArc(
                            color = colors[i],
                            startAngle = start,
                            sweepAngle = (sweep - 1f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - 2 * inset, size.height - 2 * inset),
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        )
                    }
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.inv_tab_products),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.forEachIndexed { i, level ->
                val count = health[level] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i]))
                    Text(
                        stringResource(level.label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                    Text(count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TopValueBars(products: List<Product>, onOpenProduct: (String) -> Unit) {
    val max = products.maxOf { it.stockValue }.coerceAtLeast(1.0)
    val progress = rememberGrow(products)
    val brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        products.forEach { product ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOpenProduct(product.id) },
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        product.localizedName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(product.stockValue.asMoney(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp)) {
                    val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    drawRoundRect(track, cornerRadius = r)
                    drawRoundRect(
                        brush,
                        size = Size((size.width * (product.stockValue / max) * progress).toFloat(), size.height),
                        cornerRadius = r,
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockGaugeRow(product: Product, onOpenProduct: (String) -> Unit) {
    val level = product.stockLevel
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOpenProduct(product.id) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.localizedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    productCategoryLabel(product),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(Res.string.stock_left, product.totalStock.asQuantity(), product.unit.localizedLabel()),
                style = MaterialTheme.typography.labelMedium,
                color = level.color(),
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { stockFraction(product.totalStock, product.lowStockThreshold) },
            color = level.color(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(CircleShape),
        )
    }
}

// ---------- More tab ----------

@Composable
fun PosMoreTab(
    data: DashboardData,
    access: ShopAccess,
    udhaarDue: Double?,
    onOpenProfile: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenBorrowers: () -> Unit,
    onOpenOrdersWorkspace: () -> Unit,
    onOpenOnlineOrders: () -> Unit,
    onOpenPrinter: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPosSettings: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenProductBin: () -> Unit,
    onLogout: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val canReports = access.can(ShopPermission.VIEW_REPORTS)
    val canBill = access.can(ShopPermission.BILLING)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val userName = data.userName.ifBlank { stringResource(Res.string.shopkeeper_fallback) }
        GradientCard(colors = listOf(primary, tertiary), onClick = onOpenProfile) { onColor ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(onColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Storefront, null, tint = onColor, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(Res.string.pos_more_profile_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onColor.copy(alpha = 0.9f),
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = onColor)
            }
        }
        if (canReports) {
            HighlightCard(
                colors = listOf(tertiary, primary),
                icon = Icons.Default.Insights,
                title = stringResource(Res.string.pos_more_sales_title),
                value = data.todaySales.totalValue.asMoney(),
                meta = "${data.todaySales.orderCount} ${stringResource(Res.string.sales_orders)}",
                cta = stringResource(Res.string.pos_more_sales_cta),
                onClick = onOpenReports,
            )
        }
        if (canReports || canBill) {
            HighlightCard(
                colors = listOf(Color(0xFFC62828), Color(0xFFEF6C00)),
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(Res.string.pos_more_borrowers_title),
                value = udhaarDue?.asMoney() ?: "—",
                meta = "${stringResource(Res.string.udhaar_total_due)} · ${stringResource(Res.string.pos_more_borrowers_sub)}",
                cta = stringResource(Res.string.pos_more_borrowers_cta),
                onClick = onOpenBorrowers,
            )
        }
        if (access.can(ShopPermission.SHOP_PROFILE) || access.can(ShopPermission.SETTINGS)) {
            SettingsEntryCard(
                icon = Icons.Default.Tune,
                accent = Color(0xFF00838F),
                title = stringResource(Res.string.pos_settings_title),
                subtitle = stringResource(Res.string.pos_settings_subtitle),
                onClick = onOpenPosSettings,
            )
        }
        if (access.can(ShopPermission.INVENTORY)) {
            SettingsEntryCard(
                icon = Icons.Default.DeleteSweep,
                accent = Color(0xFFC62828),
                title = stringResource(Res.string.bin_title),
                subtitle = stringResource(Res.string.bin_more_subtitle),
                onClick = onOpenProductBin,
            )
        }
        SettingsEntryCard(
            icon = Icons.Default.Settings,
            accent = Color(0xFF5E35B1),
            title = stringResource(Res.string.app_settings_title),
            subtitle = stringResource(Res.string.app_settings_subtitle),
            onClick = onOpenSettings,
        )
        var showChangePassword by remember { mutableStateOf(false) }
        SettingsEntryCard(
            icon = Icons.Default.Lock,
            accent = Color(0xFF00897B),
            title = stringResource(Res.string.pwd_change_title),
            subtitle = stringResource(Res.string.pwd_change_subtitle),
            onClick = { showChangePassword = true },
        )
        if (showChangePassword) {
            org.bhargav.pansariwala.feature.login.ChangePasswordDialog(
                onDismiss = { showChangePassword = false },
                onDone = { showChangePassword = false },
            )
        }
        Text(
            stringResource(Res.string.pos_more_shortcuts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            if (canBill) {
                add { m -> ShortcutTile(Icons.AutoMirrored.Filled.ReceiptLong, stringResource(Res.string.orders_workspace_title), Color(0xFF1565C0), onOpenOrdersWorkspace, m) }
            }
            if (access.can(ShopPermission.ONLINE_ORDERS)) {
                add { m ->
                    val newOnline = rememberNewOnlineOrderCount()
                    ShortcutTile(Icons.Default.ShoppingBag, stringResource(Res.string.online_orders_title), Color(0xFF2E7D32), onOpenOnlineOrders, m) {
                        NewOrdersBadge(newOnline)
                    }
                }
            }
            if (canBill || access.can(ShopPermission.PRINT_REPORTS) || access.can(ShopPermission.SETTINGS)) {
                add { m -> ShortcutTile(Icons.Default.Print, stringResource(Res.string.printer_title), Color(0xFF6A1B9A), onOpenPrinter, m) }
            }
            add { m -> ShortcutTile(Icons.Default.WorkspacePremium, stringResource(Res.string.sub_title), Color(0xFFEF6C00), onOpenSubscription, m) }
            add { m -> ShortcutTile(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.action_logout), Color(0xFFC62828), onLogout, m) }
        }
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile -> tile(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingsEntryCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.1f),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White) }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = accent)
        }
    }
}

@Composable
private fun GradientCard(
    colors: List<Color>,
    onClick: (() -> Unit)? = null,
    content: @Composable (onColor: Color) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(16.dp),
        ) { content(Color.White) }
    }
}

@Composable
private fun HighlightCard(
    colors: List<Color>,
    icon: ImageVector,
    title: String,
    value: String,
    meta: String,
    cta: String,
    onClick: () -> Unit,
) {
    GradientCard(colors = colors, onClick = onClick) { onColor ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = onColor.copy(alpha = 0.9f))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = onColor)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(onColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = onColor) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = onColor.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(cta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = onColor)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = onColor)
        }
    }
}

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.1f),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = Color.White) }
                Spacer(Modifier.weight(1f))
                badge?.invoke()
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
