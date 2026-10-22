package org.bhargav.pansariwala.feature.user

import org.bhargav.pansariwala.feature.support.ReportIssueButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.shape.RoundedCornerShape
import org.bhargav.pansariwala.designsystem.OnboardingFormCard
import org.bhargav.pansariwala.designsystem.OnboardingHero
import org.bhargav.pansariwala.designsystem.OnboardingHighlight
import pansariwala.shared.generated.resources.onboarding_otp_heading
import pansariwala.shared.generated.resources.onboarding_phone_heading
import pansariwala.shared.generated.resources.onboarding_phone_subheading
import pansariwala.shared.generated.resources.user_brand_title
import pansariwala.shared.generated.resources.user_onboarding_highlight_fast
import pansariwala.shared.generated.resources.user_onboarding_highlight_nearby
import pansariwala.shared.generated.resources.user_onboarding_highlight_offers
import pansariwala.shared.generated.resources.user_onboarding_tagline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import org.bhargav.pansariwala.domain.model.ShopFilter
import org.bhargav.pansariwala.domain.model.ShopType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.bhargav.pansariwala.designsystem.OtpCodeField
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.ResendOtpButton
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.account_all_orders
import pansariwala.shared.generated.resources.account_help
import pansariwala.shared.generated.resources.market_change_location
import pansariwala.shared.generated.resources.market_greeting_guest
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import pansariwala.shared.generated.resources.account_recent_three
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_otp
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.help_body
import pansariwala.shared.generated.resources.location_confirm_address
import pansariwala.shared.generated.resources.market_filter
import pansariwala.shared.generated.resources.market_greeting
import pansariwala.shared.generated.resources.market_location_unknown
import pansariwala.shared.generated.resources.market_search_products
import pansariwala.shared.generated.resources.market_shop_type_all
import pansariwala.shared.generated.resources.no_orders_yet
import pansariwala.shared.generated.resources.no_shops_nearby
import pansariwala.shared.generated.resources.profile_setup_title
import pansariwala.shared.generated.resources.user_location_access_action
import pansariwala.shared.generated.resources.user_location_access_reason_delivery
import pansariwala.shared.generated.resources.user_location_access_reason_offers
import pansariwala.shared.generated.resources.user_location_access_reason_shops
import pansariwala.shared.generated.resources.user_location_access_subtitle
import pansariwala.shared.generated.resources.user_location_access_title
import pansariwala.shared.generated.resources.user_location_permission_denied_message
import pansariwala.shared.generated.resources.user_shop_tab_continue
import pansariwala.shared.generated.resources.user_shop_tab_empty

@Composable
fun PhoneAuthScreen(
    onOtpSent: () -> Unit,
    viewModel: PhoneAuthViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.sendOtp(onOtpSent) }, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
    ) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingHero(
            title = stringResource(Res.string.user_brand_title),
            tagline = stringResource(Res.string.user_onboarding_tagline),
            heroEmoji = "🛒",
            floatingEmojis = listOf("🍅", "🥛", "🍞", "🥕", "🍎", "🧅"),
            highlights = listOf(
                OnboardingHighlight("🏪", stringResource(Res.string.user_onboarding_highlight_nearby)),
                OnboardingHighlight("🛵", stringResource(Res.string.user_onboarding_highlight_fast)),
                OnboardingHighlight("🏷️", stringResource(Res.string.user_onboarding_highlight_offers)),
            ),
            startColor = MaterialTheme.colorScheme.primary,
            endColor = MaterialTheme.colorScheme.tertiary,
        )
        OnboardingFormCard {
        Text(
            stringResource(Res.string.onboarding_phone_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(Res.string.onboarding_phone_subheading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::setPhone,
            label = { Text(stringResource(Res.string.field_phone)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        UserPrimaryButton(
            text = stringResource(Res.string.action_send_otp),
            onClick = { viewModel.sendOtp(onOtpSent) },
            enabled = !state.loading && state.phone.length >= 10,
        )
        }
    }
    }
}

@Composable
fun PhoneOtpScreen(
    onVerified: (profileComplete: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneAuthViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        title = stringResource(Res.string.onboarding_otp_heading),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = { viewModel.sendOtp() }, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.hint?.let {
                Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OtpCodeField(
                value = state.otp,
                onValueChange = viewModel::setOtp,
                enabled = !state.loading,
                onDone = { viewModel.verify { onVerified(it.profileComplete) } },
            )
            UserPrimaryButton(
                text = stringResource(Res.string.action_verify_otp),
                onClick = { viewModel.verify { onVerified(it.profileComplete) } },
                enabled = !state.loading && state.otp.length == AppConstants.OTP_LENGTH,
            )
            ResendOtpButton(resendInSec = state.resendInSec, loading = state.loading, onClick = { viewModel.sendOtp() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onOpenShop: (String) -> Unit,
    onContinueOrder: (String) -> Unit,
    onChangeLocation: () -> Unit = {},
    onShowShopOffers: (String) -> Unit = onOpenShop,
    viewModel: MarketViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deniedMessage = stringResource(Res.string.user_location_permission_denied_message)
    val cartLines by cart.lines.collectAsStateWithLifecycle()
    val cartShopId by cart.shopId.collectAsStateWithLifecycle()
    val cartShopName by cart.shopName.collectAsStateWithLifecycle()
    val cartRestored by cart.restored.collectAsStateWithLifecycle()
    val sortedShopTypes = ShopType.entries.map { it to shopTypeLabel(it) }.sortedBy { it.second.lowercase() }
    var showResumeSheet by remember { mutableStateOf(false) }
    LaunchedEffect(cartRestored) {
        if (cartRestored && !cart.resumePromptShown && cart.lines.value.isNotEmpty() && cart.shopId.value != null) {
            cart.resumePromptShown = true
            showResumeSheet = true
        }
    }
    val resumeShopId = cartShopId
    if (showResumeSheet && resumeShopId != null && cartLines.isNotEmpty()) {
        ResumeCartSheet(
            shopName = cartShopName ?: resumeShopId,
            lines = cartLines,
            subtotal = cart.subtotal,
            onResume = {
                showResumeSheet = false
                onContinueOrder(resumeShopId)
            },
            onClearCart = {
                showResumeSheet = false
                cart.clear()
            },
            onDismiss = { showResumeSheet = false },
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.onHomeVisible()
    }
    val locationSettled = !state.loading && !state.requestLocationPermission &&
        !state.showLocationDeniedDialog && !state.showLocationPrompt
    LaunchedEffect(locationSettled) {
        if (locationSettled) org.bhargav.pansariwala.notification.NotificationPromptGate.onHomeShown()
    }
    if (state.showLocationPrompt) {
        LocationAccessPromptDialog(
            title = stringResource(Res.string.user_location_access_title),
            subtitle = stringResource(Res.string.user_location_access_subtitle),
            reasons = listOf(
                "🏪" to stringResource(Res.string.user_location_access_reason_shops),
                "🛵" to stringResource(Res.string.user_location_access_reason_delivery),
                "⭐" to stringResource(Res.string.user_location_access_reason_offers),
            ),
            allowLabel = stringResource(Res.string.user_location_access_action),
            onAllow = viewModel::acceptLocationPrompt,
            onNotNow = viewModel::dismissLocationPrompt,
        )
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadProfile()
        }
    }

    RequestLocationPermission(
        trigger = state.requestLocationPermission,
        onConsumed = viewModel::consumeLocationPermissionRequest,
        onResult = viewModel::onLocationPermissionResult,
    )
    LocationPermissionDeniedDialog(
        visible = state.showLocationDeniedDialog,
        onRetry = viewModel::retryLocationPermission,
        onOpenSettings = {
            openAppLocationSettings()
            viewModel.dismissLocationDeniedDialog()
        },
        onDismiss = viewModel::dismissLocationDeniedDialog,
        message = deniedMessage,
    )

    if (state.showFilterSheet) {
        ShopFilterSheet(
            initial = ShopFilter(state.radiusKm, state.selectedShopTypes, state.sortOption),
            minRadiusKm = state.minRadiusKm,
            onApply = {
                viewModel.applyShopFilter(it)
                viewModel.hideFilterSheet()
            },
            onDismiss = viewModel::hideFilterSheet,
        )
    }

    PansariScreen(
        topBar = {
            PansariTopBar(
                onBack = null,
                actions = {
                    ReportIssueButton()
                    CartBadgeButton(
                        itemCount = CartStore.countItems(cartLines),
                        onClick = { cartShopId?.let(onContinueOrder) },
                    )
                },
            ) {
                Column {
                    Text(
                        stringResource(
                            Res.string.market_greeting,
                            state.userName.ifBlank { stringResource(Res.string.market_greeting_guest) },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.clickable(onClick = onChangeLocation),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.locationLabel.ifBlank { stringResource(Res.string.market_location_unknown) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.market_change_location),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::search, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.shops.isEmpty(),
        isRefreshing = state.loading && state.shops.isNotEmpty(),
    ) {
      Column(Modifier.fillMaxSize()) {
          Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text(stringResource(Res.string.market_search_products)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(onClick = viewModel::showFilterSheet) {
                    Icon(Icons.Default.FilterList, contentDescription = stringResource(Res.string.market_filter))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedShopTypes.isEmpty(),
                    onClick = viewModel::selectAllShopTypes,
                    label = { Text(stringResource(Res.string.market_shop_type_all)) },
                )
                sortedShopTypes.forEach { (type, label) ->
                    FilterChip(
                        selected = type in state.selectedShopTypes,
                        onClick = { viewModel.toggleShopType(type) },
                        label = { Text(label) },
                    )
                }
            }
          }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (state.shops.isEmpty() && !state.loading) {
                item(key = "empty", contentType = "empty") {
                    Text(
                        stringResource(Res.string.no_shops_nearby),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(state.shops, key = { it.id }, contentType = { "shop" }) { shop ->
                ShopDiscoveryCard(
                    shop = shop,
                    onClick = { onOpenShop(shop.id) },
                    onShowAllOffers = { onShowShopOffers(shop.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        }
      }
    }
}

@Composable
fun AccountHomeScreen(
    onRecent: (String) -> Unit,
    onOpenPayment: (String) -> Unit,
    onRetryPayment: (String) -> Unit,
    onAllOrders: () -> Unit,
    onTxns: () -> Unit,
    onHelp: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = (state as? AsyncUiState.Success)?.data
    PansariScreen(
        error = state.errorBannerOrNull(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.account_recent_three),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            SectionCard(title = stringResource(Res.string.account_recent_three)) {
                data?.payments.orEmpty().forEach { intent ->
                    PaymentAccountTile(
                        intent = intent,
                        busy = data?.paymentBusyId == intent.id,
                        onClick = { onOpenPayment(intent.id) },
                        onCheckStatus = { viewModel.checkPayment(intent.id) },
                        onRetry = { viewModel.retryPayment(intent.id, onRetryPayment) },
                    )
                    HorizontalDivider()
                }
                if (data?.recent.isNullOrEmpty() && data?.payments.isNullOrEmpty()) {
                    Text(
                        stringResource(Res.string.no_orders_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    data?.recent.orEmpty().forEach { order ->
                        OrderAccountTile(order = order, onClick = { onRecent(order.id) })
                        HorizontalDivider()
                    }
                }
            }
            UserMenuRow(
                title = stringResource(Res.string.account_all_orders),
                onClick = onAllOrders,
                trailing = (data?.ordersTotal ?: 0).toString(),
            )
            HorizontalDivider()
            UserMenuRow(
                title = stringResource(Res.string.account_transactions),
                onClick = onTxns,
                trailing = (data?.txns?.size ?: 0).toString(),
            )
            HorizontalDivider()
            UserMenuRow(title = stringResource(Res.string.account_help), onClick = onHelp)
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    PansariScreen(title = stringResource(Res.string.account_help), onBack = onBack) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.help_body))
        }
    }
}

@Composable
fun UserNotificationPrefsScreen(onBack: () -> Unit) {
    UserNotificationSettingsScreen(onBack = onBack)
}
