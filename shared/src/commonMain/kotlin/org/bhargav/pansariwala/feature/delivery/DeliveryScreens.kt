package org.bhargav.pansariwala.feature.delivery

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.platform.openExternalNavigation
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariLinkButton
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_accept
import pansariwala.shared.generated.resources.partner_job_load_failed
import pansariwala.shared.generated.resources.partner_route_header_meta
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.action_use_current_location
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_email
import pansariwala.shared.generated.resources.field_locality
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_otp
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.field_place_search
import pansariwala.shared.generated.resources.field_vehicle_photo
import pansariwala.shared.generated.resources.field_vehicle_reg
import pansariwala.shared.generated.resources.hint_address_pick_place
import pansariwala.shared.generated.resources.order_number_label
import pansariwala.shared.generated.resources.partner_register_location_required
import pansariwala.shared.generated.resources.partner_action_arrived_customer
import pansariwala.shared.generated.resources.partner_arrive_need_proximity
import pansariwala.shared.generated.resources.partner_action_arrived_store
import pansariwala.shared.generated.resources.partner_action_complete_delivery
import pansariwala.shared.generated.resources.partner_action_decline
import pansariwala.shared.generated.resources.partner_action_done
import pansariwala.shared.generated.resources.partner_action_go_offline
import pansariwala.shared.generated.resources.partner_action_login
import pansariwala.shared.generated.resources.partner_action_sign_up
import pansariwala.shared.generated.resources.partner_action_start_delivery
import pansariwala.shared.generated.resources.partner_action_take_photos
import pansariwala.shared.generated.resources.partner_action_verify_bags
import pansariwala.shared.generated.resources.partner_bag_photo
import pansariwala.shared.generated.resources.partner_call_customer
import pansariwala.shared.generated.resources.partner_call_customer_cd
import pansariwala.shared.generated.resources.checkout_delivery
import pansariwala.shared.generated.resources.checkout_discount
import pansariwala.shared.generated.resources.checkout_payable
import pansariwala.shared.generated.resources.checkout_platform_fee
import pansariwala.shared.generated.resources.checkout_subtotal
import pansariwala.shared.generated.resources.partner_payment_method
import pansariwala.shared.generated.resources.payment_method_cod
import pansariwala.shared.generated.resources.payment_method_online
import pansariwala.shared.generated.resources.partner_capture_hint
import pansariwala.shared.generated.resources.partner_capture_images
import pansariwala.shared.generated.resources.partner_customer_payment
import pansariwala.shared.generated.resources.partner_delivering
import pansariwala.shared.generated.resources.partner_delivery_complete
import pansariwala.shared.generated.resources.partner_distance_eta
import pansariwala.shared.generated.resources.partner_doc_dl
import pansariwala.shared.generated.resources.partner_doc_id
import pansariwala.shared.generated.resources.hint_vehicle_photo
import pansariwala.shared.generated.resources.partner_driver_active
import pansariwala.shared.generated.resources.partner_earnings_profile
import pansariwala.shared.generated.resources.partner_login_phone_email
import pansariwala.shared.generated.resources.partner_login_prompt
import pansariwala.shared.generated.resources.partner_new_delivery_offer
import pansariwala.shared.generated.resources.partner_notifications_empty
import pansariwala.shared.generated.resources.partner_offer_distance
import pansariwala.shared.generated.resources.partner_offer_order
import pansariwala.shared.generated.resources.partner_offer_payout
import pansariwala.shared.generated.resources.partner_offer_timer
import pansariwala.shared.generated.resources.partner_picking_up
import pansariwala.shared.generated.resources.partner_performance
import pansariwala.shared.generated.resources.partner_photo_slot
import pansariwala.shared.generated.resources.partner_profile_pic
import pansariwala.shared.generated.resources.partner_register_title
import pansariwala.shared.generated.resources.partner_route_to_customer
import pansariwala.shared.generated.resources.partner_route_to_store
import pansariwala.shared.generated.resources.partner_section_address
import pansariwala.shared.generated.resources.partner_section_contact
import pansariwala.shared.generated.resources.partner_section_payment
import pansariwala.shared.generated.resources.partner_signup_join
import pansariwala.shared.generated.resources.partner_signup_prompt
import pansariwala.shared.generated.resources.partner_submit_images
import pansariwala.shared.generated.resources.partner_summary_distance
import pansariwala.shared.generated.resources.partner_summary_payout
import pansariwala.shared.generated.resources.partner_summary_rating
import pansariwala.shared.generated.resources.partner_summary_time
import pansariwala.shared.generated.resources.partner_online_waiting
import pansariwala.shared.generated.resources.partner_offline_prompt
import pansariwala.shared.generated.resources.partner_tab_jobs
import pansariwala.shared.generated.resources.partner_tab_notifications
import pansariwala.shared.generated.resources.partner_verification_status
import pansariwala.shared.generated.resources.partner_weekly_day
import pansariwala.shared.generated.resources.partner_weekly_summary
import pansariwala.shared.generated.resources.partner_profile_details
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_theme_card

@Composable
fun PartnerLoginScreen(
    onVerified: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: PartnerLoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::sendOtp, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Spacer(Modifier.height(32.dp))
        PartnerBrandHeader()
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::setPhone,
            label = { Text(stringResource(Res.string.field_phone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.step == 0) {
            PartnerPrimaryButton(
                text = stringResource(Res.string.partner_action_login),
                onClick = viewModel::sendOtp,
                enabled = !state.loading && state.phone.length >= 10,
            )
        } else {
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::setOtp,
                label = { Text(stringResource(Res.string.field_otp)) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.hint?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall) }
            PartnerPrimaryButton(
                text = stringResource(Res.string.action_verify_otp),
                onClick = { viewModel.verify(onVerified) },
                enabled = !state.loading && state.otp.length >= 4,
            )
            PansariLinkButton(onClick = viewModel::sendOtp, enabled = !state.loading, text = stringResource(Res.string.action_resend_otp))
        }
        PansariLinkButton(onClick = onSignUp, enabled = !state.loading, text = stringResource(Res.string.partner_signup_join))
        }
    }
}

@Composable
fun PartnerRegisterScreen(
    onVerified: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit = onSignIn,
    viewModel: PartnerRegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
    )
    PansariScreen(
        topBar = {
            PartnerTopBar(
                title = stringResource(Res.string.partner_register_title),
                onBack = onBack,
                trailing = {
                    Text("?", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                },
            )
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::save, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
    ) {
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.step == 0) {
                PartnerProfileCircle(
                    base64 = state.profilePhoto,
                    onClick = viewModel::attachProfile,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    stringResource(Res.string.partner_profile_pic),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(state.name, viewModel::setName, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.email, viewModel::setEmail, label = { Text(stringResource(Res.string.field_email)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = state.placeQuery,
                    onValueChange = viewModel::setPlaceQuery,
                    label = { Text(stringResource(Res.string.field_place_search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                state.predictions.forEach { prediction ->
                    Text(
                        prediction.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlace(prediction.placeId) }
                            .padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::setAddress,
                    label = { Text(stringResource(Res.string.field_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.locality,
                    onValueChange = viewModel::setLocality,
                    label = { Text(stringResource(Res.string.field_locality)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    stringResource(Res.string.hint_address_pick_place),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = viewModel::requestLocationForAddress, enabled = !state.loading) {
                    Text(stringResource(Res.string.action_use_current_location))
                }
                OutlinedTextField(state.phone, viewModel::setPhone, label = { Text(stringResource(Res.string.field_phone)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.vehicleReg, viewModel::setVehicleReg, label = { Text(stringResource(Res.string.field_vehicle_reg)) }, modifier = Modifier.fillMaxWidth())
                PartnerDocumentRow(stringResource(Res.string.partner_doc_dl), state.dlPhoto, viewModel::attachDl)
                Text(
                    stringResource(Res.string.hint_vehicle_photo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                PartnerDocumentRow(stringResource(Res.string.field_vehicle_photo), state.vehiclePhoto, viewModel::attachVehicle)
                PartnerDocumentRow(stringResource(Res.string.partner_doc_id), state.idPhoto, viewModel::attachId)
                Text(stringResource(Res.string.partner_verification_status), fontWeight = FontWeight.SemiBold)
                PartnerVerificationBadge(verified = false)
                PartnerPrimaryButton(text = stringResource(Res.string.partner_action_sign_up), onClick = viewModel::save, enabled = !state.loading)
            } else {
                state.hint?.let { Text(it.asString()) }
                OutlinedTextField(state.otp, viewModel::setOtp, label = { Text(stringResource(Res.string.field_otp)) }, modifier = Modifier.fillMaxWidth())
                PartnerPrimaryButton(
                    text = stringResource(Res.string.action_verify_otp),
                    onClick = { viewModel.verify(onVerified) },
                    enabled = !state.loading && state.otp.length >= 4,
                )
                PansariLinkButton(onClick = viewModel::save, enabled = !state.loading, text = stringResource(Res.string.action_resend_otp))
            }
            PansariLinkButton(onClick = onSignIn, enabled = !state.loading, text = stringResource(Res.string.partner_login_prompt))
        }
        }
    }
}

@Composable
fun PartnerHomeScreen(
    onNavigateToStore: (String) -> Unit,
    onResumeJob: (org.bhargav.pansariwala.domain.model.Order) -> Unit,
    onEarnings: () -> Unit,
    viewModel: PartnerHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var homeTab by remember { mutableStateOf(0) }
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
    )
    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
        isRefreshing = state.refreshing,
        topBar = {
            PartnerHomeTopBar(
                title = state.profile?.name ?: stringResource(Res.string.partner_driver_active),
                profilePhotoBase64 = state.profile?.profilePhoto,
                onProfileClick = onEarnings,
            )
        },
    ) {
    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PartnerOnlineBanner(
                online = state.online,
                onToggle = viewModel::setOnline,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PartnerHomeTabChip(
                    label = stringResource(Res.string.partner_tab_jobs),
                    selected = homeTab == 0,
                    onClick = { homeTab = 0 },
                    modifier = Modifier.weight(1f),
                )
                PartnerHomeTabChip(
                    label = stringResource(Res.string.partner_tab_notifications),
                    selected = homeTab == 1,
                    onClick = { homeTab = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (homeTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            PartnerAcceptedOrdersSection(
                                jobs = state.acceptedJobs,
                                onResume = onResumeJob,
                            )
                        }
                        item {
                            PartnerAvailableOrdersSection(
                                offers = state.availableOffers,
                                online = state.online,
                                secondsLeftFor = viewModel::offerSecondsRemaining,
                                onAccept = { offerId -> viewModel.acceptOfferById(offerId, onNavigateToStore) },
                                onDecline = viewModel::rejectOfferById,
                                onGoOnline = { viewModel.setOnline(true) },
                            )
                        }
                        item {
                            PartnerMapPlaceholder(
                                title = state.profile?.name ?: stringResource(Res.string.partner_driver_active),
                                subtitle = if (state.online) {
                                    stringResource(Res.string.partner_online_waiting)
                                } else {
                                    stringResource(Res.string.partner_offline_prompt)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                lat = state.lat,
                                lng = state.lng,
                                onCurrentLocationClick = viewModel::refreshCurrentLocation,
                                fetchingLocation = state.fetchingLocation,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(Res.string.partner_notifications_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (state.online) {
                PartnerDangerButton(
                    text = stringResource(Res.string.partner_action_go_offline),
                    onClick = { viewModel.setOnline(false) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        state.incomingOffer?.let { offer ->
            PartnerOfferDialog(
                offer = offer,
                secondsLeft = state.offerSecondsLeft,
                onAccept = { viewModel.acceptOffer(onNavigateToStore) },
                onDecline = viewModel::rejectOffer,
            )
        }
        if (state.showOfferTakenSheet) {
            PartnerOfferTakenSheet(onOkay = viewModel::dismissOfferTakenSheet)
        }
    }
    }
}

@Composable
private fun PartnerHomeTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PartnerOfferDialog(
    offer: org.bhargav.pansariwala.domain.model.DeliveryOffer,
    secondsLeft: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PartnerJobOfferCard(
            offer = offer,
            secondsLeft = secondsLeft,
            onAccept = onAccept,
            onDecline = onDecline,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
fun PartnerNavigateToStoreScreen(
    orderId: String,
    onArrived: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerJobViewModel = koinViewModel(),
    deviceLocation: DeviceLocation = koinInject(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    var origin by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(orderId) {
        origin = runCatching { deviceLocation.currentOrDefault() }.getOrNull()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        val shopLabel = order?.shopName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.partner_route_to_store)
        val km = order?.shopDistanceKm() ?: 0.0
        val mins = order?.deliveryDurationMin ?: 6
        val headerMeta = order?.let {
            stringResource(
                Res.string.partner_route_header_meta,
                shopLabel,
                ((km * 10).toInt() / 10.0).toString(),
                mins.toString(),
            )
        }
        PartnerTopBar(
            title = stringResource(Res.string.partner_route_to_store),
            onBack = onBack,
        )
        if (headerMeta != null) {
            PartnerRouteMetaBar(text = headerMeta)
        }
        when {
            state.loading && order == null -> PartnerJobLoadingState(contentModifier = Modifier.weight(1f))
            state.error != null && order == null -> PartnerJobErrorState(
                message = state.error!!.asString(),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
            order != null -> {
                val shopLat = order.shopLat ?: AppConstants.DEFAULT_MAP_LAT
                val shopLng = order.shopLng ?: AppConstants.DEFAULT_MAP_LNG
                PartnerMapPlaceholder(
                    title = shopLabel,
                    subtitle = order.shopAddress,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fillHeight = true,
                    lat = shopLat,
                    lng = shopLng,
                    originLat = origin?.lat,
                    originLng = origin?.lng,
                    showCaption = false,
                )
                PartnerLocationCard(
                    title = shopLabel,
                    distanceEta = stringResource(
                        Res.string.partner_distance_eta,
                        ((km * 10).toInt() / 10.0).toString(),
                        mins.toString(),
                    ),
                    address = order.shopAddress.orEmpty(),
                    onNavigate = { openExternalNavigation(shopLat, shopLng) },
                )
                state.error?.let {
                    Text(
                        it.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_arrived_store),
                    onClick = { viewModel.arrivedAtStore(onArrived) },
                    modifier = Modifier.padding(16.dp),
                    enabled = !state.submitting,
                )
            }
            else -> PartnerJobErrorState(
                message = stringResource(Res.string.partner_job_load_failed),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
        }
    }
}

private fun org.bhargav.pansariwala.domain.model.Order.shopDistanceKm(): Double =
    totalDistanceKm?.div(2) ?: quote?.deliveryDistanceKm ?: 1.0

@Composable
fun PartnerPickupItemsScreen(
    orderId: String,
    onVerifyBags: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: PartnerJobViewModel = koinViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        PartnerTopBar(title = stringResource(Res.string.partner_picking_up), onBack = onBack)
        when {
            state.loading && state.order == null -> PartnerJobLoadingState(contentModifier = Modifier.weight(1f))
            state.error != null && state.order == null -> PartnerJobErrorState(
                message = state.error!!.asString(),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
            state.order != null -> {
                val order = state.order!!
                Text(
                    stringResource(Res.string.order_number_label, order.id),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                )
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    order.items.forEach { PartnerProductRow(it) }
                }
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_verify_bags),
                    onClick = { viewModel.verifyBags(onVerifyBags) },
                    modifier = Modifier.padding(16.dp),
                    enabled = !state.submitting,
                )
            }
            else -> PartnerJobErrorState(
                message = stringResource(Res.string.partner_job_load_failed),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PartnerCapturePhotosScreen(
    orderId: String,
    onPhotosReady: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: PartnerJobViewModel = koinViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val darkBg = Color(0xFF1A1A1A)
    Column(Modifier.fillMaxSize().background(darkBg)) {
        PartnerTopBar(
            title = stringResource(
                if (state.captureStep == 0) Res.string.partner_capture_images else Res.string.partner_submit_images,
            ),
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.captureStep == 0) {
                Text(
                    stringResource(Res.string.partner_capture_hint),
                    color = Color.White.copy(alpha = 0.85f),
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_photo_slot, "1"),
                    state.photoOne.isNotBlank(),
                    onClick = { viewModel.attachPhoto(1) },
                    dark = true,
                    imageBase64 = state.photoOne,
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_photo_slot, "2"),
                    state.photoTwo.isNotBlank(),
                    onClick = { viewModel.attachPhoto(2) },
                    dark = true,
                    imageBase64 = state.photoTwo,
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_take_photos),
                    onClick = { viewModel.verifyBags {} },
                    enabled = state.photoOne.isNotBlank() && state.photoTwo.isNotBlank() && !state.submitting,
                )
            } else {
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_bag_photo, "1"),
                    true,
                    onClick = {},
                    dark = true,
                    imageBase64 = state.photoOne,
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_bag_photo, "2"),
                    true,
                    onClick = {},
                    dark = true,
                    imageBase64 = state.photoTwo,
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_start_delivery),
                    onClick = { viewModel.submitPickup(onPhotosReady) },
                    enabled = !state.submitting,
                )
            }
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun PartnerDeliverToCustomerScreen(
    orderId: String,
    onArrived: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: PartnerJobViewModel = koinViewModel(),
    deviceLocation: DeviceLocation = koinInject(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    var origin by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(orderId) {
        origin = runCatching { deviceLocation.currentOrDefault() }.getOrNull()
    }
    val destLat = order?.customerLat ?: AppConstants.DEFAULT_MAP_LAT
    val destLng = order?.customerLng ?: AppConstants.DEFAULT_MAP_LNG
    val withinRadius = rememberWithinArrivalRadius(destLat, destLng, deviceLocation)
    // DEV ONLY — remove with DevTripleTapUnlock.kt
    var forceArriveEnabled by remember { mutableStateOf(false) }
    val canArrive = withinRadius || forceArriveEnabled
    Column(modifier = Modifier.fillMaxSize()) {
        val customerLabel = order?.customerName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.partner_route_to_customer)
        val km = order?.quote?.deliveryDistanceKm ?: order?.totalDistanceKm?.div(2) ?: 2.0
        val mins = order?.deliveryDurationMin ?: 8
        val headerMeta = order?.let {
            stringResource(
                Res.string.partner_route_header_meta,
                customerLabel,
                ((km * 10).toInt() / 10.0).toString(),
                mins.toString(),
            )
        }
        PartnerTopBar(
            title = stringResource(Res.string.partner_delivering),
            onBack = onBack,
        )
        if (headerMeta != null) {
            PartnerRouteMetaBar(text = headerMeta)
        }
        when {
            state.loading && order == null -> PartnerJobLoadingState(contentModifier = Modifier.weight(1f))
            state.error != null && order == null -> PartnerJobErrorState(
                message = state.error!!.asString(),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
            order != null -> {
                PartnerMapPlaceholder(
                    title = customerLabel,
                    subtitle = order.deliveryAddress,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fillHeight = true,
                    lat = destLat,
                    lng = destLng,
                    originLat = origin?.lat ?: order.shopLat,
                    originLng = origin?.lng ?: order.shopLng,
                    showCaption = false,
                )
                PartnerLocationCard(
                    title = customerLabel,
                    distanceEta = stringResource(
                        Res.string.partner_distance_eta,
                        ((km * 10).toInt() / 10.0).toString(),
                        mins.toString(),
                    ),
                    address = order.deliveryAddress.orEmpty(),
                    onNavigate = { openExternalNavigation(destLat, destLng) },
                )
                state.error?.let {
                    Text(
                        it.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                if (!canArrive) {
                    Text(
                        stringResource(Res.string.partner_arrive_need_proximity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Box(modifier = Modifier.padding(16.dp)) {
                    PartnerPrimaryButton(
                        text = stringResource(Res.string.partner_action_arrived_customer),
                        onClick = { viewModel.arrivedAtCustomer(onArrived) },
                        enabled = canArrive && !state.submitting,
                    )
                    // DEV ONLY — triple-tap disabled button area to unlock; remove with DevTripleTapUnlock.kt
                    if (!canArrive) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .devTripleTapToUnlock { forceArriveEnabled = true },
                        )
                    }
                }
            }
            else -> PartnerJobErrorState(
                message = stringResource(Res.string.partner_job_load_failed),
                onRetry = { viewModel.load(orderId) },
                onBack = onBack,
                contentModifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PartnerCustomerPaymentScreen(
    orderId: String,
    onComplete: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: PartnerJobViewModel = koinViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    val uriHandler = LocalUriHandler.current
    var otp by remember { mutableStateOf("") }
    val otpReady = otp.length == AppConstants.DELIVERY_OTP_LENGTH
    val pickupPhotos = order?.visiblePickupPhotos.orEmpty()
    Column(Modifier.fillMaxSize()) {
        PartnerTopBar(
            title = stringResource(Res.string.partner_customer_payment),
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            order?.let {
                SectionBlock(stringResource(Res.string.partner_section_address), it.deliveryAddress.orEmpty())
                val phone = it.customerPhone.orEmpty()
                val callLabel = stringResource(Res.string.partner_call_customer_cd)
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.partner_section_contact),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            listOf(stringResource(Res.string.partner_call_customer), phone)
                                .filter { part -> part.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(
                        onClick = { uriHandler.openUri("tel:$phone") },
                        enabled = phone.isNotBlank(),
                        modifier = Modifier.semantics { contentDescription = callLabel },
                    ) {
                        Text("📞", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Card(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(Res.string.partner_section_payment),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val quote = it.quote
                        if (quote != null) {
                            PaymentDetailLine(stringResource(Res.string.checkout_subtotal), quote.itemsSubtotal.asMoney())
                            if (quote.discount > 0) {
                                PaymentDetailLine(
                                    stringResource(Res.string.checkout_discount),
                                    "-${quote.discount.asMoney()}",
                                )
                            }
                            PaymentDetailLine(stringResource(Res.string.checkout_platform_fee), quote.platformFee.asMoney())
                            PaymentDetailLine(stringResource(Res.string.checkout_delivery), quote.deliveryCharge.asMoney())
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            PaymentDetailLine(
                                stringResource(Res.string.checkout_payable),
                                quote.payable.asMoney(),
                                bold = true,
                            )
                        } else {
                            PaymentDetailLine(
                                stringResource(Res.string.checkout_payable),
                                it.totalValue.asMoney(),
                                bold = true,
                            )
                        }
                        PaymentDetailLine(
                            stringResource(Res.string.partner_payment_method),
                            stringResource(
                                if (it.paymentMethod == "ONLINE") Res.string.payment_method_online
                                else Res.string.payment_method_cod,
                            ),
                        )
                    }
                }
                if (pickupPhotos.isNotEmpty()) {
                    PickupPhotoStrip(
                        photos = pickupPhotos,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                DeliveryOtpInput(
                    value = otp,
                    onValueChange = { otp = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        }
        PartnerPrimaryButton(
            text = stringResource(Res.string.partner_action_complete_delivery),
            onClick = { viewModel.completeDelivery(otp, onComplete) },
            modifier = Modifier.padding(16.dp),
            enabled = order != null && !state.submitting && otpReady,
        )
    }
}

@Composable
private fun SectionBlock(title: String, body: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PaymentDetailLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun PartnerDeliveryCompleteScreen(
    orderId: String,
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: PartnerJobViewModel = koinViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        PartnerTopBar(
            title = stringResource(Res.string.partner_delivery_complete),
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.order?.let { order ->
                Text(stringResource(Res.string.order_number_label, order.id), fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.partner_summary_payout, (order.partnerPayoutInr ?: order.quote?.deliveryCharge ?: 0.0).asMoney()))
                Text(stringResource(Res.string.partner_summary_distance, ((order.totalDistanceKm ?: 0.0).let { (it * 10).toInt() / 10.0 }).toString()))
                Text(stringResource(Res.string.partner_summary_time, (order.deliveryDurationMin ?: 1).toString()))
                Text(stringResource(Res.string.partner_summary_rating))
                Text("★★★★★", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.headlineSmall)
            }
        }
        PartnerPrimaryButton(
            text = stringResource(Res.string.partner_action_done),
            onClick = onDone,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun PartnerEarningsScreen(
    onBack: () -> Unit,
    onLanguage: () -> Unit = {},
    onTheme: () -> Unit = {},
    onNotifications: () -> Unit = {},
    viewModel: PartnerEarningsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        topBar = {
            PartnerTopBar(
                title = stringResource(Res.string.partner_earnings_profile),
                onBack = onBack,
            )
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::load, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        state.profile?.let { profile ->
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (profile.profilePhoto.isNotBlank()) {
                    Base64ImageThumbnail(
                        base64 = profile.profilePhoto,
                        contentDescription = profile.name,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(profile.name.take(1), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column {
                    Text(profile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(profile.phone, style = MaterialTheme.typography.bodySmall)
                    Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        state.earnings?.let { earnings ->
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.partner_earnings_profile), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    EarningRow("Today", "₹${earnings.todayEarnings.asMoney()}")
                    EarningRow("Total", "₹${earnings.totalEarnings.asMoney()}")
                    EarningRow("Deliveries", earnings.deliveredCount.toString())
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.partner_performance), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    EarningRow("Acceptance Rate", "${earnings.acceptanceRatePercent}%")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        state.profile?.let { profile ->
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.partner_profile_details), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    EarningRow("Name", profile.name)
                    EarningRow("Address", profile.address)
                    EarningRow("Vehicle", profile.vehicleReg)
                    EarningRow("Status", if (profile.verified) "Verified" else "Onboarding")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        state.earnings?.let { earnings ->
            if (earnings.weeklyEarnings.isNotEmpty()) {
                Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
            ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(Res.string.partner_weekly_summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider()
                        earnings.weeklyEarnings.forEach { day ->
                            EarningRow(day.dayLabel, "₹${day.amount.asMoney()}")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            stringResource(Res.string.settings_language_card),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onLanguage).padding(16.dp),
            fontWeight = FontWeight.Medium,
        )
        HorizontalDivider()
        Text(
            stringResource(Res.string.settings_theme_card),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTheme).padding(16.dp),
            fontWeight = FontWeight.Medium,
        )
        HorizontalDivider()
        Text(
            stringResource(Res.string.settings_notification_card),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNotifications).padding(16.dp),
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EarningRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}
