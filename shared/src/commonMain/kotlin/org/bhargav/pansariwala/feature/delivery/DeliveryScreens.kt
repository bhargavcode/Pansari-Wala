package org.bhargav.pansariwala.feature.delivery

import org.bhargav.pansariwala.designsystem.appDialogProperties
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
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.PhotoUploadUi
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.DrivingRoute
import org.bhargav.pansariwala.platform.fetchDrivingRouteDetails
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.platform.openExternalNavigation
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.OtpCodeField
import org.bhargav.pansariwala.designsystem.PansariLinkButton
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.ResendOtpButton
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.feature.user.InteractiveStarRating
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.partner_onboarding
import pansariwala.shared.generated.resources.partner_verified
import pansariwala.shared.generated.resources.partner_row_status
import pansariwala.shared.generated.resources.partner_row_vehicle
import pansariwala.shared.generated.resources.partner_percent_value
import pansariwala.shared.generated.resources.partner_row_acceptance
import pansariwala.shared.generated.resources.partner_row_deliveries
import pansariwala.shared.generated.resources.partner_row_total
import pansariwala.shared.generated.resources.partner_today
import pansariwala.shared.generated.resources.return_partner_action
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
import pansariwala.shared.generated.resources.partner_action_login
import pansariwala.shared.generated.resources.partner_action_sign_up
import pansariwala.shared.generated.resources.partner_action_submit_out_for_delivery
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
import pansariwala.shared.generated.resources.partner_onboarding_highlight_earn
import pansariwala.shared.generated.resources.partner_onboarding_highlight_flexible
import pansariwala.shared.generated.resources.partner_onboarding_highlight_nearby
import pansariwala.shared.generated.resources.partner_onboarding_tagline
import pansariwala.shared.generated.resources.partner_onboarding_title
import pansariwala.shared.generated.resources.onboarding_otp_heading
import pansariwala.shared.generated.resources.onboarding_phone_heading
import pansariwala.shared.generated.resources.onboarding_phone_subheading
import org.bhargav.pansariwala.designsystem.OnboardingFormCard
import org.bhargav.pansariwala.designsystem.OnboardingHero
import org.bhargav.pansariwala.designsystem.OnboardingHighlight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.lerp
import pansariwala.shared.generated.resources.partner_signup_prompt
import pansariwala.shared.generated.resources.partner_location_access_action
import pansariwala.shared.generated.resources.partner_location_access_reason_eta
import pansariwala.shared.generated.resources.partner_location_access_reason_jobs
import pansariwala.shared.generated.resources.partner_location_access_reason_tracking
import pansariwala.shared.generated.resources.partner_location_access_subtitle
import pansariwala.shared.generated.resources.partner_location_access_title
import pansariwala.shared.generated.resources.partner_submit_images
import pansariwala.shared.generated.resources.partner_summary_distance
import pansariwala.shared.generated.resources.partner_summary_payout
import pansariwala.shared.generated.resources.partner_rate_customer_comment
import pansariwala.shared.generated.resources.partner_rate_customer_required
import pansariwala.shared.generated.resources.partner_rate_customer_submit
import pansariwala.shared.generated.resources.partner_rate_customer_title
import pansariwala.shared.generated.resources.partner_rate_customer_title_generic
import pansariwala.shared.generated.resources.partner_summary_time
import pansariwala.shared.generated.resources.partner_online_waiting
import pansariwala.shared.generated.resources.partner_offline_prompt
import pansariwala.shared.generated.resources.partner_verification_status
import pansariwala.shared.generated.resources.partner_weekly_day
import pansariwala.shared.generated.resources.partner_weekly_summary
import pansariwala.shared.generated.resources.partner_profile_details
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_theme_card

@Composable
fun PartnerHomeScreen(
    onNavigateToStore: (String) -> Unit,
    onResumeJob: (org.bhargav.pansariwala.domain.model.Order) -> Unit,
    onEarnings: () -> Unit,
    viewModel: PartnerHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val locationSettled = !state.loading && !state.requestLocationPermission &&
        !state.showLocationDeniedDialog && !state.showLocationPrompt
    LaunchedEffect(locationSettled) {
        if (locationSettled) org.bhargav.pansariwala.notification.NotificationPromptGate.onHomeShown()
    }
    if (state.showLocationPrompt) {
        org.bhargav.pansariwala.feature.user.LocationAccessPromptDialog(
            title = stringResource(Res.string.partner_location_access_title),
            subtitle = stringResource(Res.string.partner_location_access_subtitle),
            reasons = listOf(
                "🛵" to stringResource(Res.string.partner_location_access_reason_jobs),
                "📍" to stringResource(Res.string.partner_location_access_reason_tracking),
                "⏱️" to stringResource(Res.string.partner_location_access_reason_eta),
            ),
            allowLabel = stringResource(Res.string.partner_location_access_action),
            onAllow = viewModel::acceptLocationPrompt,
            onNotNow = viewModel::dismissLocationPrompt,
        )
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
                profilePhotoUrl = state.profile?.profilePhoto,
                online = state.online,
                onToggleOnline = viewModel::setOnline,
                onProfileClick = onEarnings,
            )
        },
    ) {
    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "earnings_profile") {
                        PartnerEarningsProfileCard(
                            profile = state.profile,
                            earnings = state.earnings,
                            period = state.earningsPeriod,
                            range = state.earningsRange,
                            loading = state.earningsLoading,
                            onSelectPeriod = viewModel::selectEarningsPeriod,
                            onProfileClick = onEarnings,
                        )
                    }
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
                            partnerName = state.profile?.name,
                        )
                    }
                }
            }
            PartnerMapPlaceholder(
                title = state.profile?.name ?: stringResource(Res.string.partner_driver_active),
                subtitle = if (state.online) {
                    stringResource(Res.string.partner_online_waiting)
                } else {
                    stringResource(Res.string.partner_offline_prompt)
                },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .height(220.dp),
                lat = state.lat,
                lng = state.lng,
                onCurrentLocationClick = viewModel::refreshCurrentLocation,
                fetchingLocation = state.fetchingLocation,
            )
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
        if (state.showEarningsRangePicker) {
            org.bhargav.pansariwala.feature.reports.CustomRangeDialog(
                onDismiss = viewModel::dismissEarningsRangePicker,
                onConfirm = viewModel::setEarningsCustomRange,
            )
        }
    }
    }
}

@Composable
private fun PartnerOfferDialog(
    offer: org.bhargav.pansariwala.domain.model.DeliveryOffer,
    secondsLeft: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = appDialogProperties(usePlatformDefaultWidth = false)) {
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
    val origin = rememberPartnerOrigin(orderId, deviceLocation)
    val shopLabel = order?.shopName?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.partner_route_to_store)
    val road = rememberRoadRoute(
        origin?.lat,
        origin?.lng,
        order?.shopLat ?: AppConstants.DEFAULT_MAP_LAT,
        order?.shopLng ?: AppConstants.DEFAULT_MAP_LNG,
    )
    val km = road?.distanceKm ?: order?.shopDistanceKm() ?: 0.0
    val mins = road?.durationMinutes ?: order?.deliveryDurationMin ?: 6
    val headerMeta = order?.let {
        stringResource(
            Res.string.partner_route_header_meta,
            shopLabel,
            ((km * 10).toInt() / 10.0).toString(),
            mins.toString(),
        )
    }
    PansariScreen(
        topBar = {
            Column {
                PartnerTopBar(
                    title = stringResource(Res.string.partner_route_to_store),
                    onBack = onBack,
                )
                if (headerMeta != null) {
                    PartnerRouteMetaBar(text = headerMeta)
                }
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.load(orderId) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.loading && order == null,
    ) {
        order?.let { loaded ->
            val shopLat = loaded.shopLat ?: AppConstants.DEFAULT_MAP_LAT
            val shopLng = loaded.shopLng ?: AppConstants.DEFAULT_MAP_LNG
            Column(Modifier.fillMaxSize()) {
                PartnerMapPlaceholder(
                    title = shopLabel,
                    subtitle = loaded.shopAddress,
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
                    address = loaded.shopAddress.orEmpty(),
                    onNavigate = { openExternalNavigation(shopLat, shopLng) },
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_arrived_store),
                    onClick = { viewModel.arrivedAtStore(onArrived) },
                    modifier = Modifier.padding(16.dp),
                    enabled = !state.submitting,
                )
            }
        }
    }
}

/** Road distance / ETA for the header and card; null until a router answers (then order estimates are shown). */
@Composable
private fun rememberRoadRoute(
    originLat: Double?,
    originLng: Double?,
    destLat: Double,
    destLng: Double,
): DrivingRoute? {
    var route by remember(originLat, originLng, destLat, destLng) { mutableStateOf<DrivingRoute?>(null) }
    LaunchedEffect(originLat, originLng, destLat, destLng) {
        if (originLat == null || originLng == null) return@LaunchedEffect
        route = fetchDrivingRouteDetails(originLat, originLng, destLat, destLng)
            .takeIf { it.distanceMeters != null }
    }
    return route
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
    val order = state.order
    PansariScreen(
        title = stringResource(Res.string.partner_picking_up),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.load(orderId) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.loading && order == null,
    ) {
        order?.let { loaded ->
            Column(Modifier.fillMaxSize()) {
                Text(
                    stringResource(Res.string.order_number_label, loaded.id),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                )
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    loaded.deliveringItems.forEach { PartnerProductRow(it, packed = true) }
                }
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_verify_bags),
                    onClick = { viewModel.verifyBags(onVerifyBags) },
                    modifier = Modifier.padding(16.dp),
                    enabled = !state.submitting,
                )
            }
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
    PansariScreen(
        title = stringResource(
            if (state.captureStep == 0) Res.string.partner_capture_images else Res.string.partner_submit_images,
        ),
        onBack = onBack,
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.order == null,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(darkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.captureStep == 0) {
                val attachOne = rememberImageSourceLauncher(ImageUploadFeature.DELIVERY_PACKET) {
                    viewModel.attachPhoto(1, it)
                }
                val attachTwo = rememberImageSourceLauncher(ImageUploadFeature.DELIVERY_PACKET) {
                    viewModel.attachPhoto(2, it)
                }
                Text(
                    stringResource(Res.string.partner_capture_hint),
                    color = Color.White.copy(alpha = 0.85f),
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_photo_slot, "1"),
                    state.photoOne.isNotBlank(),
                    onClick = attachOne,
                    dark = true,
                    imageUrl = state.photoOne,
                    upload = state.photoOneUpload,
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_photo_slot, "2"),
                    state.photoTwo.isNotBlank(),
                    onClick = attachTwo,
                    dark = true,
                    imageUrl = state.photoTwo,
                    upload = state.photoTwoUpload,
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_submit_out_for_delivery),
                    onClick = { viewModel.submitPickup(onPhotosReady) },
                    enabled = state.photoOne.isNotBlank() && state.photoTwo.isNotBlank() && !state.submitting &&
                        !state.photoOneUpload.uploading && !state.photoTwoUpload.uploading,
                )
            } else {
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_bag_photo, "1"),
                    true,
                    onClick = {},
                    dark = true,
                    imageUrl = state.photoOne,
                )
                PartnerPhotoSlot(
                    stringResource(Res.string.partner_bag_photo, "2"),
                    true,
                    onClick = {},
                    dark = true,
                    imageUrl = state.photoTwo,
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_submit_out_for_delivery),
                    onClick = { viewModel.submitPickup(onPhotosReady) },
                    enabled = !state.submitting,
                )
            }
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
    val origin = rememberPartnerOrigin(orderId, deviceLocation)
    val destLat = order?.customerLat ?: AppConstants.DEFAULT_MAP_LAT
    val destLng = order?.customerLng ?: AppConstants.DEFAULT_MAP_LNG
    val withinRadius = rememberWithinArrivalRadius(destLat, destLng, deviceLocation)
    // DEV ONLY — remove with DevTripleTapUnlock.kt
    var forceArriveEnabled by remember { mutableStateOf(false) }
    val canArrive = withinRadius || forceArriveEnabled
    val customerLabel = order?.customerName?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.partner_route_to_customer)
    val road = rememberRoadRoute(origin?.lat ?: order?.shopLat, origin?.lng ?: order?.shopLng, destLat, destLng)
    val km = road?.distanceKm ?: order?.quote?.deliveryDistanceKm ?: order?.totalDistanceKm?.div(2) ?: 2.0
    val mins = road?.durationMinutes ?: order?.deliveryDurationMin ?: 8
    val headerMeta = order?.let {
        stringResource(
            Res.string.partner_route_header_meta,
            customerLabel,
            ((km * 10).toInt() / 10.0).toString(),
            mins.toString(),
        )
    }
    PansariScreen(
        topBar = {
            Column {
                PartnerTopBar(
                    title = stringResource(Res.string.partner_delivering),
                    onBack = onBack,
                )
                if (headerMeta != null) {
                    PartnerRouteMetaBar(text = headerMeta)
                }
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.load(orderId) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.loading && order == null,
    ) {
        order?.let { loaded ->
            Column(Modifier.fillMaxSize()) {
                PartnerMapPlaceholder(
                    title = customerLabel,
                    subtitle = loaded.deliveryAddress,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fillHeight = true,
                    lat = destLat,
                    lng = destLng,
                    originLat = origin?.lat ?: loaded.shopLat,
                    originLng = origin?.lng ?: loaded.shopLng,
                    showCaption = false,
                )
                PartnerLocationCard(
                    title = customerLabel,
                    distanceEta = stringResource(
                        Res.string.partner_distance_eta,
                        ((km * 10).toInt() / 10.0).toString(),
                        mins.toString(),
                    ),
                    address = loaded.deliveryAddress.orEmpty(),
                    onNavigate = { openExternalNavigation(destLat, destLng) },
                )
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
        }
    }
}

@Composable
fun PartnerCustomerPaymentScreen(
    orderId: String,
    onComplete: () -> Unit,
    onReturning: () -> Unit = {},
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
    var showReturnDialog by remember { mutableStateOf(false) }
    if (showReturnDialog) {
        ReturnToShopDialog(
            busy = state.submitting,
            onConfirm = { reason -> viewModel.returnToShop(reason) { showReturnDialog = false; onReturning() } },
            onDismiss = { showReturnDialog = false },
        )
    }
    PansariScreen(
        title = stringResource(Res.string.partner_customer_payment),
        onBack = onBack,
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && order == null,
    ) {
        Column(Modifier.fillMaxSize()) {
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
        }
        PartnerPrimaryButton(
            text = stringResource(Res.string.partner_action_complete_delivery),
            onClick = { viewModel.completeDelivery(otp, onComplete) },
            modifier = Modifier.padding(16.dp),
            enabled = order != null && !state.submitting && otpReady,
        )
        TextButton(
            onClick = { showReturnDialog = true },
            enabled = order != null && !state.submitting,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Text(
                stringResource(Res.string.return_partner_action),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        }
        }
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
    val existing = state.order?.customerRatingByPartner
    var stars by remember(existing) { mutableStateOf(existing?.stars ?: 0) }
    var comment by remember(existing) { mutableStateOf(existing?.comment.orEmpty()) }
    PansariScreen(
        title = stringResource(Res.string.partner_delivery_complete),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.load(orderId) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.loading && state.order == null,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.order?.let { order ->
                Text(stringResource(Res.string.order_number_label, order.id), fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.partner_summary_payout, (order.partnerPayoutInr ?: order.quote?.deliveryCharge ?: 0.0).asMoney()))
                Text(stringResource(Res.string.partner_summary_distance, ((order.totalDistanceKm ?: 0.0).let { (it * 10).toInt() / 10.0 }).toString()))
                Text(stringResource(Res.string.partner_summary_time, (order.deliveryDurationMin ?: 1).toString()))
                HorizontalDivider()
                Text(
                    order.customerName?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(Res.string.partner_rate_customer_title, it) }
                        ?: stringResource(Res.string.partner_rate_customer_title_generic),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                InteractiveStarRating(
                    stars = stars,
                    onStarsChange = { stars = it },
                    enabled = !state.submitting,
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= AppConstants.RATING_COMMENT_MAX_CHARS) comment = it },
                    label = { Text(stringResource(Res.string.partner_rate_customer_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !state.submitting,
                )
                if (stars == 0) {
                    Text(
                        stringResource(Res.string.partner_rate_customer_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PartnerPrimaryButton(
                text = stringResource(Res.string.partner_rate_customer_submit),
                onClick = { viewModel.rateCustomer(stars, comment, onDone) },
                enabled = stars in 1..5 && !state.submitting && state.order != null,
            )
        }
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

        val changeProfilePhoto = rememberImageSourceLauncher(
            ImageUploadFeature.PARTNER_PROFILE_PHOTO,
            viewModel::changeProfilePhoto,
        )
        state.profile?.let { profile ->
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PartnerProfileCircle(
                    imageUrl = profile.profilePhoto,
                    onClick = changeProfilePhoto,
                    showEditBadge = true,
                    uploading = state.uploadingPhoto,
                    localPreviewBytes = state.profilePhotoPreviewBytes,
                )
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
                    EarningRow(stringResource(Res.string.partner_today), earnings.todayEarnings.asMoney())
                    EarningRow(stringResource(Res.string.partner_row_total), earnings.totalEarnings.asMoney())
                    EarningRow(stringResource(Res.string.partner_row_deliveries), earnings.deliveredCount.toString())
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
                    EarningRow(stringResource(Res.string.partner_row_acceptance), stringResource(Res.string.partner_percent_value, earnings.acceptanceRatePercent))
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
                    EarningRow(stringResource(Res.string.field_name), profile.name)
                    EarningRow(stringResource(Res.string.partner_section_address), profile.address)
                    EarningRow(stringResource(Res.string.partner_row_vehicle), profile.vehicleReg)
                    EarningRow(stringResource(Res.string.partner_row_status), stringResource(if (profile.verified) Res.string.partner_verified else Res.string.partner_onboarding))
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
                            EarningRow(day.dayLabel, day.amount.asMoney())
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
        HorizontalDivider()
        org.bhargav.pansariwala.feature.account.AccountClosureSection(
            org.bhargav.pansariwala.feature.account.ClosableAccount.PARTNER,
            Modifier.padding(16.dp),
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
