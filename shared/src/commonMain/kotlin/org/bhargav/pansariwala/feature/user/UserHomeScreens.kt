package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariSearchTopBar
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.account_all_orders
import pansariwala.shared.generated.resources.account_help
import pansariwala.shared.generated.resources.account_recent_orders
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.action_continue
import pansariwala.shared.generated.resources.action_logout
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_otp
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.help_body
import pansariwala.shared.generated.resources.market_radius
import pansariwala.shared.generated.resources.market_search_hint
import pansariwala.shared.generated.resources.no_shops_nearby
import pansariwala.shared.generated.resources.notify_delivery
import pansariwala.shared.generated.resources.notify_offers
import pansariwala.shared.generated.resources.phone_auth_subtitle
import pansariwala.shared.generated.resources.phone_auth_title
import pansariwala.shared.generated.resources.profile_setup_title
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_radius
import pansariwala.shared.generated.resources.settings_theme_card
import pansariwala.shared.generated.resources.shop_distance
import pansariwala.shared.generated.resources.shop_rating
import pansariwala.shared.generated.resources.tab_market
import kotlin.math.roundToInt

@Composable
fun PhoneAuthScreen(
    onVerified: (profileComplete: Boolean) -> Unit,
    viewModel: PhoneAuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.phone_auth_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.phone_auth_subtitle))
        OutlinedTextField(state.phone, viewModel::setPhone, label = { Text(stringResource(Res.string.field_phone)) }, modifier = Modifier.fillMaxWidth())
        if (state.step == 0) {
            Button(onClick = viewModel::sendOtp, enabled = !state.loading && state.phone.length >= 10) {
                Text(stringResource(Res.string.action_send_otp))
            }
        } else {
            OutlinedTextField(state.otp, viewModel::setOtp, label = { Text(stringResource(Res.string.field_otp)) }, modifier = Modifier.fillMaxWidth())
            state.hint?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = { viewModel.verify { onVerified(it.profileComplete) } },
                enabled = !state.loading && state.otp.length >= 4,
            ) { Text(stringResource(Res.string.action_verify_otp)) }
            TextButton(onClick = viewModel::sendOtp, enabled = !state.loading) {
                Text(stringResource(Res.string.action_resend_otp))
            }
        }
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        if (state.loading) CircularProgressIndicator()
    }
}

@Composable
fun ProfileSetupScreen(
    onDone: () -> Unit,
    viewModel: ProfileSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.profile_setup_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(state.name, viewModel::setName, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(state.address, viewModel::setAddress, label = { Text(stringResource(Res.string.field_address)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.save(onDone) }, enabled = !state.loading) { Text(stringResource(Res.string.action_continue)) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun MarketScreen(
    onOpenShop: (String) -> Unit,
    viewModel: MarketViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PansariSearchTopBar(
            title = stringResource(Res.string.tab_market),
            searchQuery = state.query,
            searchLabel = stringResource(Res.string.market_search_hint),
            onSearchChange = {
                viewModel.setQuery(it)
                viewModel.search()
            },
            onBack = null,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(Res.string.market_radius, state.radiusKm.roundToInt().toString()))
            Slider(
                value = state.radiusKm.toFloat(),
                onValueChange = { viewModel.setRadius(it.toDouble()) },
                valueRange = AppConstants.MIN_SEARCH_RADIUS_KM.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
            )
            if (state.shops.isEmpty() && !state.loading) Text(stringResource(Res.string.no_shops_nearby))
            state.shops.forEach { shop -> ShopRow(shop, onClick = { onOpenShop(shop.id) }) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ShopRow(shop: MarketplaceShop, onClick: () -> Unit) {
    SectionCard(title = shop.name, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(stringResource(Res.string.shop_rating, shop.rating.toString(), shop.ratingCount))
        Text(stringResource(Res.string.shop_distance, shop.distanceKm.let { (it * 10).roundToInt() / 10.0 }.toString()))
    }
}

@Composable
fun AccountHomeScreen(
    onRecent: (String) -> Unit,
    onAllOrders: () -> Unit,
    onTxns: () -> Unit,
    onHelp: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(title = stringResource(Res.string.account_recent_orders)) {
            state.recent.forEach { order ->
                OrderRow(order, onClick = { onRecent(order.id) })
            }
        }
        SectionCard(title = stringResource(Res.string.account_all_orders), modifier = Modifier.clickable(onClick = onAllOrders)) {
            Text("${state.orders.size}")
        }
        SectionCard(title = stringResource(Res.string.account_transactions), modifier = Modifier.clickable(onClick = onTxns)) {
            Text("${state.txns.size}")
        }
        SectionCard(title = stringResource(Res.string.account_help), modifier = Modifier.clickable(onClick = onHelp)) {
            Text(stringResource(Res.string.help_body))
        }
    }
}

@Composable
fun OrderRow(order: Order, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(order.shopName ?: order.id)
        Text(order.status.name)
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PansariTopBar(
            title = stringResource(Res.string.account_help),
            onBack = onBack,
        )
        Text(stringResource(Res.string.help_body))
    }
}

@Composable
fun UserSettingsHub(
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
    viewModel: UserSettingsViewModel = koinViewModel(),
) {
    val radius by viewModel.radius.collectAsStateWithLifecycle(AppConstants.DEFAULT_SEARCH_RADIUS_KM)
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(title = stringResource(Res.string.settings_radius)) {
            Text(stringResource(Res.string.market_radius, radius.roundToInt().toString()))
            Slider(
                value = radius.toFloat(),
                onValueChange = { viewModel.setRadius(it.toDouble()) },
                valueRange = AppConstants.MIN_SEARCH_RADIUS_KM.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
            )
        }
        SectionCard(title = stringResource(Res.string.settings_language_card), modifier = Modifier.clickable(onClick = onLanguage)) {
            Text(AppLanguage.ENGLISH.displayLabel)
        }
        SectionCard(title = stringResource(Res.string.settings_notification_card), modifier = Modifier.clickable(onClick = onNotifications)) {}
        SectionCard(title = stringResource(Res.string.settings_theme_card), modifier = Modifier.clickable(onClick = onTheme)) {}
        TextButton(onClick = { viewModel.logout(onLogout) }) { Text(stringResource(Res.string.action_logout)) }
    }
}

@Composable
fun UserNotificationPrefsScreen(onBack: () -> Unit) {
    org.bhargav.pansariwala.feature.settings.SettingsScreen(onBack = onBack)
}
