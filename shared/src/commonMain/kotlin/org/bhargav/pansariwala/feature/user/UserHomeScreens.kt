package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import org.bhargav.pansariwala.domain.model.ShopSortOption
import org.bhargav.pansariwala.domain.model.ShopType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariTopBar
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
import pansariwala.shared.generated.resources.account_recent_three
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.action_logout
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_otp
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.help_body
import pansariwala.shared.generated.resources.location_confirm_address
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.market_filter
import pansariwala.shared.generated.resources.market_filter_title
import pansariwala.shared.generated.resources.market_greeting
import pansariwala.shared.generated.resources.market_location_unknown
import pansariwala.shared.generated.resources.market_radius_range
import pansariwala.shared.generated.resources.market_search_products
import pansariwala.shared.generated.resources.market_shop_types
import pansariwala.shared.generated.resources.market_sort_distance_asc
import pansariwala.shared.generated.resources.market_sort_distance_desc
import pansariwala.shared.generated.resources.market_sort_name_asc
import pansariwala.shared.generated.resources.market_sort_name_desc
import pansariwala.shared.generated.resources.market_sort_rating
import pansariwala.shared.generated.resources.no_orders_yet
import pansariwala.shared.generated.resources.no_shops_nearby
import pansariwala.shared.generated.resources.profile_payment_header
import pansariwala.shared.generated.resources.profile_setup_title
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_radius
import pansariwala.shared.generated.resources.settings_theme_card
import pansariwala.shared.generated.resources.user_location_access_action
import pansariwala.shared.generated.resources.user_location_access_continue
import pansariwala.shared.generated.resources.user_location_access_reason_delivery
import pansariwala.shared.generated.resources.user_location_access_reason_offers
import pansariwala.shared.generated.resources.user_location_access_reason_shops
import pansariwala.shared.generated.resources.user_location_access_subtitle
import pansariwala.shared.generated.resources.user_location_access_title
import pansariwala.shared.generated.resources.user_location_coords
import pansariwala.shared.generated.resources.user_location_permission_denied_message
import pansariwala.shared.generated.resources.user_shop_tab_continue
import pansariwala.shared.generated.resources.user_shop_tab_empty
import kotlin.math.roundToInt

private fun Double.formatCoord(): String = ((this * 1000).roundToInt() / 1000.0).toString()

@Composable
fun PhoneAuthScreen(
    onVerified: (profileComplete: Boolean) -> Unit,
    viewModel: PhoneAuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::sendOtp, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        UserBrandHeader()
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::setPhone,
            label = { Text(stringResource(Res.string.field_phone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.step == 0) {
            UserPrimaryButton(
                text = stringResource(Res.string.action_send_otp),
                onClick = viewModel::sendOtp,
                enabled = !state.loading && state.phone.length >= 10,
            )
        } else {
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::setOtp,
                label = { Text(stringResource(Res.string.field_otp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            state.hint?.let {
                Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            UserPrimaryButton(
                text = stringResource(Res.string.action_verify_otp),
                onClick = { viewModel.verify { onVerified(it.profileComplete) } },
                enabled = !state.loading && state.otp.length >= 4,
            )
            TextButton(onClick = viewModel::sendOtp, enabled = !state.loading) {
                Text(stringResource(Res.string.action_resend_otp))
            }
        }
        }
    }
}

@Composable
fun UserLocationAccessScreen(
    onDone: () -> Unit,
    viewModel: UserLocationAccessViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deniedMessage = stringResource(Res.string.user_location_permission_denied_message)

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

    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::requestLocationAccess, onDismiss = viewModel::dismissError)
        },
        isLoading = state.fetchingLocation || state.saving,
    ) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        UserLocationOnboardingHero(fetching = state.fetchingLocation)
        Text(
            stringResource(Res.string.user_location_access_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            stringResource(Res.string.user_location_access_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        UserLocationReasonRow(stringResource(Res.string.user_location_access_reason_shops), "🏪")
        UserLocationReasonRow(stringResource(Res.string.user_location_access_reason_delivery), "🛵")
        UserLocationReasonRow(stringResource(Res.string.user_location_access_reason_offers), "⭐")
        if (state.lat != null && state.lng != null) {
            Text(
                stringResource(
                    Res.string.user_location_coords,
                    state.lat!!.formatCoord(),
                    state.lng!!.formatCoord(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!state.locationPermissionGranted || state.lat == null) {
            UserPrimaryButton(
                text = stringResource(Res.string.user_location_access_action),
                onClick = viewModel::requestLocationAccess,
                enabled = !state.fetchingLocation,
            )
        }
        UserPrimaryButton(
            text = stringResource(Res.string.user_location_access_continue),
            onClick = { viewModel.continueToHome(onDone) },
            enabled = !state.saving && state.lat != null && state.lng != null && state.locationPermissionGranted,
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onOpenShop: (String) -> Unit,
    onContinueOrder: (String) -> Unit,
    onChangeLocation: () -> Unit = {},
    viewModel: MarketViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deniedMessage = stringResource(Res.string.user_location_permission_denied_message)
    val cartLines by cart.lines.collectAsStateWithLifecycle()
    val cartShopId by cart.shopId.collectAsStateWithLifecycle()
    val cartShopName by cart.shopName.collectAsStateWithLifecycle()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftSort by remember(state.showFilterSheet) { mutableStateOf(state.sortOption) }
    var draftTypes by remember(state.showFilterSheet) { mutableStateOf(state.selectedShopTypes) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.onHomeVisible()
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
        ModalBottomSheet(
            onDismissRequest = viewModel::hideFilterSheet,
            sheetState = filterSheetState,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.market_filter_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                listOf(
                    ShopSortOption.DISTANCE_ASC to Res.string.market_sort_distance_asc,
                    ShopSortOption.DISTANCE_DESC to Res.string.market_sort_distance_desc,
                    ShopSortOption.RATING_DESC to Res.string.market_sort_rating,
                    ShopSortOption.NAME_ASC to Res.string.market_sort_name_asc,
                    ShopSortOption.NAME_DESC to Res.string.market_sort_name_desc,
                ).forEach { (option, labelRes) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { draftSort = option },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = draftSort == option, onClick = { draftSort = option })
                        Text(stringResource(labelRes))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(Res.string.market_shop_types), fontWeight = FontWeight.SemiBold)
                ShopType.entries.forEach { type ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            draftTypes = draftTypes.toMutableSet().apply {
                                if (contains(type)) remove(type) else add(type)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = type in draftTypes, onCheckedChange = null)
                        Text(shopTypeLabel(type))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        draftSort = ShopSortOption.DISTANCE_ASC
                        draftTypes = emptySet()
                    }) { Text(stringResource(Res.string.action_clear)) }
                    UserPrimaryButton(
                        text = stringResource(Res.string.action_apply),
                        onClick = {
                            viewModel.setSortOption(draftSort)
                            viewModel.setFilterShopTypes(draftTypes)
                            viewModel.hideFilterSheet()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::search, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.shops.isEmpty(),
        isRefreshing = state.loading && state.shops.isNotEmpty(),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(Res.string.market_greeting, state.userName.ifBlank { "there" }),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.locationLabel.ifBlank { stringResource(Res.string.market_location_unknown) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onChangeLocation),
                )
            }
            if (cartLines.isNotEmpty() && cartShopId != null) {
                ContinueLastOrderCard(
                    shopName = cartShopName ?: cartShopId.orEmpty(),
                    lines = cartLines,
                    subtotal = cart.subtotal,
                    onContinue = { onContinueOrder(cartShopId!!) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                ShopType.entries.forEach { type ->
                    FilterChip(
                        selected = type in state.selectedShopTypes,
                        onClick = { viewModel.toggleShopType(type) },
                        label = { Text(shopTypeLabel(type)) },
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.shops.isEmpty() && !state.loading) {
                    Text(stringResource(Res.string.no_shops_nearby))
                }
                state.shops.forEach { shop ->
                    ShopDiscoveryCard(shop = shop, onClick = { onOpenShop(shop.id) })
                }
            }
        }
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
                if (data?.recent.isNullOrEmpty()) {
                    Text(
                        stringResource(Res.string.no_orders_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    data.recent.forEach { order ->
                        OrderAccountTile(order = order, onClick = { onRecent(order.id) })
                        HorizontalDivider()
                    }
                }
            }
            UserMenuRow(
                title = stringResource(Res.string.account_all_orders),
                onClick = onAllOrders,
                trailing = (data?.orders?.size ?: 0).toString(),
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
fun UserSettingsHub(
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
    viewModel: UserSettingsViewModel = koinViewModel(),
) {
    val radius by viewModel.radius.collectAsStateWithLifecycle(AppConstants.DEFAULT_SEARCH_RADIUS_KM)
    val settings by viewModel.settings.collectAsStateWithLifecycle(org.bhargav.pansariwala.settings.AppUserSettings())
    PansariScreen {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            stringResource(Res.string.profile_payment_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SectionCard(title = stringResource(Res.string.settings_radius)) {
            Text(stringResource(Res.string.market_radius_range, AppConstants.MIN_SEARCH_RADIUS_KM.roundToInt().toString(), AppConstants.MAX_SEARCH_RADIUS_KM.roundToInt().toString()))
            Slider(
                value = radius.toFloat(),
                onValueChange = { viewModel.setRadius(it.toDouble()) },
                valueRange = AppConstants.MIN_SEARCH_RADIUS_KM.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        UserMenuRow(
            title = stringResource(Res.string.settings_language_card),
            onClick = onLanguage,
            trailing = settings.language.displayLabel,
        )
        HorizontalDivider()
        UserMenuRow(title = stringResource(Res.string.settings_notification_card), onClick = onNotifications)
        HorizontalDivider()
        UserMenuRow(title = stringResource(Res.string.settings_theme_card), onClick = onTheme)
        HorizontalDivider()
        TextButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(Res.string.action_logout))
        }
        }
    }
}

@Composable
fun UserNotificationPrefsScreen(onBack: () -> Unit) {
    UserNotificationSettingsScreen(onBack = onBack)
}
