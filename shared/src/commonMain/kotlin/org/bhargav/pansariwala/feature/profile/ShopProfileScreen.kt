package org.bhargav.pansariwala.feature.profile

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.OtpStepSection
import org.bhargav.pansariwala.designsystem.OtpStepState
import pansariwala.shared.generated.resources.upi_not_set
import pansariwala.shared.generated.resources.upi_add
import pansariwala.shared.generated.resources.upi_change
import pansariwala.shared.generated.resources.upi_sheet_title
import pansariwala.shared.generated.resources.upi_sheet_hint
import pansariwala.shared.generated.resources.upi_verify_save
import pansariwala.shared.generated.resources.action_send_otp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.OutlinedCard
import pansariwala.shared.generated.resources.sub_profile_row
import pansariwala.shared.generated.resources.feature_barcode_scanner
import pansariwala.shared.generated.resources.profile_features_master_managed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.OfferDiscountType
import org.bhargav.pansariwala.api.OfferScope
import org.bhargav.pansariwala.api.ShopDetailsDto
import org.bhargav.pansariwala.api.ShopFeaturesDto
import org.bhargav.pansariwala.api.ShopHoursDayDto
import org.bhargav.pansariwala.api.ShopOfferDto
import org.bhargav.pansariwala.api.ShopStaffDto
import org.bhargav.pansariwala.api.ShopTypeOptionDto
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.LocalFloatingKeyboardEligible
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.TimePickerField
import org.bhargav.pansariwala.designsystem.formatClockTime
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import pansariwala.shared.generated.resources.riders_title
import org.bhargav.pansariwala.domain.model.ShopReview
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.feature.user.shopTypeLabel
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import pansariwala.shared.generated.resources.profile_change_photo
import pansariwala.shared.generated.resources.profile_field_location
import pansariwala.shared.generated.resources.profile_locating
import pansariwala.shared.generated.resources.profile_location_confirm
import pansariwala.shared.generated.resources.profile_location_not_set
import pansariwala.shared.generated.resources.profile_shop_photo
import pansariwala.shared.generated.resources.profile_use_current_location
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.currency_symbol
import pansariwala.shared.generated.resources.shop_order_rules_title
import pansariwala.shared.generated.resources.shop_min_order_label
import pansariwala.shared.generated.resources.shop_min_order_none
import pansariwala.shared.generated.resources.shop_min_order_hint
import pansariwala.shared.generated.resources.shop_free_delivery_label
import pansariwala.shared.generated.resources.shop_free_delivery_above
import pansariwala.shared.generated.resources.shop_free_delivery_off
import pansariwala.shared.generated.resources.shop_free_delivery_amount
import pansariwala.shared.generated.resources.shop_free_delivery_hint
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.action_yes
import pansariwala.shared.generated.resources.feature_barcode_search
import pansariwala.shared.generated.resources.feature_inventory_alerts
import pansariwala.shared.generated.resources.feature_online_orders
import pansariwala.shared.generated.resources.feature_report_generation
import pansariwala.shared.generated.resources.feature_voice_search
import pansariwala.shared.generated.resources.voice_auto_add
import pansariwala.shared.generated.resources.keyboard_floating_hint
import pansariwala.shared.generated.resources.keyboard_floating_toggle
import pansariwala.shared.generated.resources.keyboard_settings_title
import pansariwala.shared.generated.resources.voice_auto_add_hint
import pansariwala.shared.generated.resources.offer_active
import pansariwala.shared.generated.resources.offer_delete_confirm
import pansariwala.shared.generated.resources.offer_field_description
import pansariwala.shared.generated.resources.offer_field_min_bill
import pansariwala.shared.generated.resources.offer_field_title
import pansariwala.shared.generated.resources.offer_field_value
import pansariwala.shared.generated.resources.offer_products_search
import pansariwala.shared.generated.resources.offer_products_title
import pansariwala.shared.generated.resources.offer_scope_bill
import pansariwala.shared.generated.resources.offer_scope_product
import pansariwala.shared.generated.resources.offer_summary_flat
import pansariwala.shared.generated.resources.offer_summary_min_bill
import pansariwala.shared.generated.resources.offer_summary_percent
import pansariwala.shared.generated.resources.offer_summary_products
import pansariwala.shared.generated.resources.offer_type_flat
import pansariwala.shared.generated.resources.offer_type_percent
import pansariwala.shared.generated.resources.offers_add
import pansariwala.shared.generated.resources.offers_edit
import pansariwala.shared.generated.resources.offers_empty
import pansariwala.shared.generated.resources.offers_subtitle
import pansariwala.shared.generated.resources.offers_title
import pansariwala.shared.generated.resources.perm_admin
import pansariwala.shared.generated.resources.perm_billing
import pansariwala.shared.generated.resources.perm_inventory
import pansariwala.shared.generated.resources.perm_manage_staff
import pansariwala.shared.generated.resources.perm_offers
import pansariwala.shared.generated.resources.perm_online_orders
import pansariwala.shared.generated.resources.perm_print_reports
import pansariwala.shared.generated.resources.perm_ratings
import pansariwala.shared.generated.resources.perm_settings
import pansariwala.shared.generated.resources.perm_shop_profile
import pansariwala.shared.generated.resources.perm_toggle_features
import pansariwala.shared.generated.resources.perm_view_reports
import pansariwala.shared.generated.resources.perm_visitors
import pansariwala.shared.generated.resources.profile_access_title
import pansariwala.shared.generated.resources.profile_edit_shop
import pansariwala.shared.generated.resources.profile_features_subtitle
import pansariwala.shared.generated.resources.profile_features_title
import pansariwala.shared.generated.resources.profile_field_address
import pansariwala.shared.generated.resources.profile_field_city
import pansariwala.shared.generated.resources.profile_field_discount
import pansariwala.shared.generated.resources.profile_field_name
import pansariwala.shared.generated.resources.profile_field_owner_email
import pansariwala.shared.generated.resources.profile_field_owner_name
import pansariwala.shared.generated.resources.profile_field_shop_types
import pansariwala.shared.generated.resources.shop_types_primary_label
import pansariwala.shared.generated.resources.shop_types_select_hint
import pansariwala.shared.generated.resources.profile_field_owner_phone
import pansariwala.shared.generated.resources.profile_field_radius
import pansariwala.shared.generated.resources.master_hours_end
import pansariwala.shared.generated.resources.master_hours_start
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.shop_delivery_until_clear
import pansariwala.shared.generated.resources.shop_delivery_until_hint
import pansariwala.shared.generated.resources.shop_delivery_until_label
import pansariwala.shared.generated.resources.shop_delivery_until_none
import pansariwala.shared.generated.resources.shop_home_delivery_hint
import pansariwala.shared.generated.resources.shop_home_delivery_label
import pansariwala.shared.generated.resources.shop_hours_edit
import pansariwala.shared.generated.resources.shop_hours_not_set
import pansariwala.shared.generated.resources.shop_hours_range
import pansariwala.shared.generated.resources.shop_hours_title
import pansariwala.shared.generated.resources.profile_field_rating
import pansariwala.shared.generated.resources.profile_field_registration
import pansariwala.shared.generated.resources.profile_field_state
import pansariwala.shared.generated.resources.profile_field_tax
import pansariwala.shared.generated.resources.profile_field_upi
import pansariwala.shared.generated.resources.profile_field_zip
import pansariwala.shared.generated.resources.profile_payments_enabled
import pansariwala.shared.generated.resources.profile_rating_value
import pansariwala.shared.generated.resources.profile_role_admin
import pansariwala.shared.generated.resources.profile_role_executive
import pansariwala.shared.generated.resources.pos_settings_payments_hint
import pansariwala.shared.generated.resources.pos_settings_payments
import pansariwala.shared.generated.resources.pos_settings_visibility_hint
import pansariwala.shared.generated.resources.pos_settings_visibility
import pansariwala.shared.generated.resources.pos_settings_delivery
import pansariwala.shared.generated.resources.pos_settings_title
import pansariwala.shared.generated.resources.staff_username_checking
import pansariwala.shared.generated.resources.staff_username_available
import pansariwala.shared.generated.resources.staff_username_taken
import pansariwala.shared.generated.resources.staff_username_check_failed
import pansariwala.shared.generated.resources.profile_shop_active
import pansariwala.shared.generated.resources.profile_shop_details
import pansariwala.shared.generated.resources.profile_tab_offers
import pansariwala.shared.generated.resources.profile_tab_ratings
import pansariwala.shared.generated.resources.profile_tab_shop
import pansariwala.shared.generated.resources.profile_tab_staff
import pansariwala.shared.generated.resources.profile_tab_visitors
import pansariwala.shared.generated.resources.profile_title
import pansariwala.shared.generated.resources.ratings_edit_reply
import pansariwala.shared.generated.resources.ratings_empty
import pansariwala.shared.generated.resources.ratings_reply
import pansariwala.shared.generated.resources.ratings_reply_hint
import pansariwala.shared.generated.resources.ratings_shop_reply
import pansariwala.shared.generated.resources.ratings_title
import pansariwala.shared.generated.resources.staff_access_title
import pansariwala.shared.generated.resources.staff_active
import pansariwala.shared.generated.resources.staff_add
import pansariwala.shared.generated.resources.staff_delete_confirm
import pansariwala.shared.generated.resources.staff_disabled
import pansariwala.shared.generated.resources.staff_edit
import pansariwala.shared.generated.resources.staff_empty
import pansariwala.shared.generated.resources.staff_field_name
import pansariwala.shared.generated.resources.staff_field_password
import pansariwala.shared.generated.resources.staff_field_password_keep
import pansariwala.shared.generated.resources.staff_field_username
import pansariwala.shared.generated.resources.staff_managed_by_master
import pansariwala.shared.generated.resources.staff_subtitle
import pansariwala.shared.generated.resources.staff_title
import pansariwala.shared.generated.resources.visitors_daily_title
import pansariwala.shared.generated.resources.visitors_range_all
import pansariwala.shared.generated.resources.visitors_range_month
import pansariwala.shared.generated.resources.visitors_range_today
import pansariwala.shared.generated.resources.visitors_range_week
import pansariwala.shared.generated.resources.visitors_subtitle
import pansariwala.shared.generated.resources.visitors_title
import pansariwala.shared.generated.resources.visitors_unique
import pansariwala.shared.generated.resources.visitors_visits
import org.bhargav.pansariwala.i18n.localizedName

@Composable
fun ShopProfileScreen(
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    /** True = "POS settings": only the shop-level settings cards; false = profile with staff/offers/ratings. */
    settingsMode: Boolean = false,
    viewModel: ShopProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = state.message?.asString()
    LaunchedEffect(savedMessage) {
        if (savedMessage != null) {
            snackbar.showSnackbar(savedMessage)
            viewModel.consumeMessage()
        }
    }

    val tabs = if (settingsMode) emptyList() else state.tabs - ProfileTab.SHOP
    val activeTab = if (settingsMode) ProfileTab.SHOP else state.tab.takeIf { it in tabs } ?: tabs.firstOrNull()
    LaunchedEffect(activeTab, state.profile != null) {
        if (activeTab != null && activeTab != state.tab && state.profile != null) viewModel.selectTab(activeTab)
    }
    PansariScreen(
        title = stringResource(if (settingsMode) Res.string.pos_settings_title else Res.string.profile_title),
        onBack = onBack,
        error = state.error.toErrorBanner(retryable = state.profile == null),
        onErrorAction = { viewModel.dismissError(); if (state.profile == null) viewModel.refresh() },
        isLoading = state.loading && state.profile == null,
        isRefreshing = state.saving,
    ) {
        val profile = state.profile ?: return@PansariScreen
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = if (settingsMode) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!settingsMode) item { MeCard(profile.me, state.access, Modifier.padding(top = 16.dp)) }
            if (tabs.size > 1) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tabs.forEach { tab ->
                            FilterChip(
                                selected = activeTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                label = { Text(stringResource(tab.label())) },
                            )
                        }
                    }
                }
            }
            when (activeTab) {
                null -> Unit
                ProfileTab.SHOP -> {
                    posSettingsItems(profile.shop, state, viewModel)
                    item { SubscriptionEntryCard(onOpenSubscription) }
                    item {
                        val voiceAutoAdd by viewModel.voiceAutoAdd.collectAsStateWithLifecycle()
                        FeaturesCard(profile.shop.features, voiceAutoAdd, viewModel::setVoiceAutoAdd)
                    }
                    item(key = "pos_keyboard") {
                        if (LocalFloatingKeyboardEligible.current) {
                            val floatingKeyboard by viewModel.floatingKeyboard.collectAsStateWithLifecycle()
                            KeyboardCard(floatingKeyboard, viewModel::setFloatingKeyboard)
                        }
                    }
                    if (settingsMode && state.access.isAdmin) {
                        item(key = "pos_account_closure") {
                            org.bhargav.pansariwala.feature.account.AccountClosureSection(
                                org.bhargav.pansariwala.feature.account.ClosableAccount.SHOP,
                                Modifier.padding(bottom = 16.dp),
                            )
                        }
                    }
                }
                ProfileTab.STAFF -> staffItems(state.staff, state.access, viewModel)
                ProfileTab.RIDERS -> riderItems(profile.shop.id)
                ProfileTab.OFFERS -> offerItems(state.offers, viewModel)
                ProfileTab.RATINGS -> ratingItems(profile.shop, state.ratings, viewModel)
                ProfileTab.VISITORS -> item { VisitorsCard(state, viewModel::setVisitorRange) }
            }
            item { Box(Modifier.padding(bottom = 24.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    state.detailsDraft?.let { draft ->
        ShopDetailsDialog(
            draft = draft,
            shopTypeOptions = state.shopTypeOptions,
            onChange = viewModel::updateDetailsDraft,
            onToggleShopType = viewModel::toggleDraftShopType,
            onMakePrimary = viewModel::makeDraftShopTypePrimary,
            onSave = viewModel::saveDetails,
            onDismiss = viewModel::cancelEditDetails,
        )
    }
    state.upiDraft?.let { draft ->
        val otp by viewModel.upiOtp.state.collectAsStateWithLifecycle()
        UpiEditSheet(
            draft = draft,
            otp = otp,
            onDraft = viewModel::onUpiDraft,
            onSendOtp = viewModel::sendUpiOtp,
            onCode = viewModel.upiOtp::onCode,
            onVerify = viewModel::verifyUpi,
            onDismiss = viewModel::cancelEditUpi,
        )
    }
    state.hoursDraft?.let { draft ->
        OperatingHoursDialog(draft, viewModel::updateHoursDay, viewModel::saveHours, viewModel::cancelEditHours)
    }
    state.orderRulesDraft?.let { draft ->
        OrderRulesDialog(draft, viewModel::updateOrderRulesDraft, viewModel::saveOrderRules, viewModel::cancelEditOrderRules)
    }
    state.staffDraft?.let { draft ->
        StaffDialog(draft, state.access, viewModel::updateStaffDraft, viewModel::saveStaff, viewModel::cancelStaffDraft)
    }
    state.offerDraft?.let { draft ->
        OfferDialog(draft, state.products, viewModel::updateOfferDraft, viewModel::saveOffer, viewModel::cancelOfferDraft)
    }
    state.replyDraft?.let { draft ->
        AppAlertDialog(
            onDismissRequest = viewModel::cancelReply,
            title = { Text(stringResource(Res.string.ratings_reply)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.ratings_reply_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = draft.text,
                        onValueChange = viewModel::updateReply,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    )
                }
            },
            confirmButton = { Button(onClick = viewModel::saveReply) { Text(stringResource(Res.string.action_save)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelReply) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
    state.confirmDeleteStaff?.let { staff ->
        ConfirmDialog(
            text = stringResource(Res.string.staff_delete_confirm, staff.displayName),
            onConfirm = viewModel::confirmDeleteStaff,
            onDismiss = { viewModel.requestDeleteStaff(null) },
        )
    }
    if (state.confirmUseCurrentLocation) {
        ConfirmDialog(
            text = stringResource(Res.string.profile_location_confirm),
            onConfirm = viewModel::confirmUseCurrentLocation,
            onDismiss = viewModel::cancelUseCurrentLocation,
        )
    }
    RequestLocationPermission(
        trigger = state.requestLocationPermission,
        onConsumed = viewModel::onLocationPermissionRequestConsumed,
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
    state.confirmDeleteOffer?.let { offer ->
        ConfirmDialog(
            text = stringResource(Res.string.offer_delete_confirm, offer.title),
            onConfirm = viewModel::confirmDeleteOffer,
            onDismiss = { viewModel.requestDeleteOffer(null) },
        )
    }
}

private fun ProfileTab.label(): StringResource = when (this) {
    ProfileTab.SHOP -> Res.string.profile_tab_shop
    ProfileTab.STAFF -> Res.string.profile_tab_staff
    ProfileTab.RIDERS -> Res.string.riders_title
    ProfileTab.OFFERS -> Res.string.profile_tab_offers
    ProfileTab.RATINGS -> Res.string.profile_tab_ratings
    ProfileTab.VISITORS -> Res.string.profile_tab_visitors
}

fun ShopPermission.label(): StringResource = when (this) {
    ShopPermission.ADMIN -> Res.string.perm_admin
    ShopPermission.BILLING -> Res.string.perm_billing
    ShopPermission.ONLINE_ORDERS -> Res.string.perm_online_orders
    ShopPermission.INVENTORY -> Res.string.perm_inventory
    ShopPermission.VIEW_REPORTS -> Res.string.perm_view_reports
    ShopPermission.PRINT_REPORTS -> Res.string.perm_print_reports
    ShopPermission.TOGGLE_FEATURES -> Res.string.perm_toggle_features
    ShopPermission.SETTINGS -> Res.string.perm_settings
    ShopPermission.SHOP_PROFILE -> Res.string.perm_shop_profile
    ShopPermission.MANAGE_STAFF -> Res.string.perm_manage_staff
    ShopPermission.OFFERS -> Res.string.perm_offers
    ShopPermission.RATINGS -> Res.string.perm_ratings
    ShopPermission.VISITORS -> Res.string.perm_visitors
}

@Composable
private fun MeCard(me: ShopStaffDto, access: ShopAccess, modifier: Modifier = Modifier) {
    val role = stringResource(if (access.isAdmin) Res.string.profile_role_admin else Res.string.profile_role_executive)
    SectionCard(title = me.displayName.ifBlank { me.username }, subtitle = "$role · ${me.username}", modifier = modifier.fillMaxWidth()) {
        if (!access.isAdmin) {
            Text(stringResource(Res.string.profile_access_title), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShopPermission.grantable.filter { it in access.permissions }.forEach { perm ->
                    AssistChip(onClick = {}, label = { Text(stringResource(perm.label())) })
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.posSettingsItems(
    shop: ShopDetailsDto,
    state: ShopProfileUiState,
    viewModel: ShopProfileViewModel,
) {
    val canEdit = state.access.can(ShopPermission.SHOP_PROFILE)
    item(key = "pos_shop_details") { ShopDetailsCard(shop, state, viewModel, canEdit) }
    item(key = "pos_delivery") {
        SectionCard(
            title = stringResource(Res.string.pos_settings_delivery),
            modifier = Modifier.fillMaxWidth(),
            action = editAction(canEdit, viewModel::startEditDetails),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(Res.string.profile_field_radius, shop.deliveryRadiusKm.formatOne())
                DetailRow(
                    Res.string.shop_delivery_until_label,
                    formatClockTime(shop.deliveryUntil) ?: stringResource(Res.string.shop_delivery_until_none),
                )
                DetailRow(Res.string.profile_field_discount, shop.discountPercent.formatOne())
                HorizontalDivider()
                ToggleRow(stringResource(Res.string.shop_home_delivery_label), shop.homeDeliveryEnabled, canEdit, viewModel::setHomeDeliveryEnabled)
                SettingHint(stringResource(Res.string.shop_home_delivery_hint))
            }
        }
    }
    item(key = "pos_visibility") {
        SectionCard(title = stringResource(Res.string.pos_settings_visibility), modifier = Modifier.fillMaxWidth()) {
            ToggleRow(stringResource(Res.string.profile_shop_active), shop.active, canEdit, viewModel::setShopActive)
            SettingHint(stringResource(Res.string.pos_settings_visibility_hint))
        }
    }
    item(key = "pos_payments") {
        SectionCard(title = stringResource(Res.string.pos_settings_payments), modifier = Modifier.fillMaxWidth()) {
            ToggleRow(stringResource(Res.string.profile_payments_enabled), shop.paymentsEnabled, canEdit, viewModel::setPaymentsEnabled)
            SettingHint(stringResource(Res.string.pos_settings_payments_hint))
            DetailRow(Res.string.profile_field_upi, shop.upiId.ifBlank { stringResource(Res.string.upi_not_set) })
            if (canEdit) {
                TextButton(onClick = viewModel::startEditUpi) {
                    Text(stringResource(if (shop.upiId.isBlank()) Res.string.upi_add else Res.string.upi_change))
                }
            }
        }
    }
    item(key = "pos_order_rules") {
        SectionCard(
            title = stringResource(Res.string.shop_order_rules_title),
            modifier = Modifier.fillMaxWidth(),
            action = editAction(canEdit, viewModel::startEditOrderRules),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(
                    Res.string.shop_min_order_label,
                    if (shop.minOrderValueInr > 0.0) shop.minOrderValueInr.asMoney() else stringResource(Res.string.shop_min_order_none),
                )
                DetailRow(
                    Res.string.shop_free_delivery_label,
                    if (shop.freeDeliveryEnabled) {
                        stringResource(Res.string.shop_free_delivery_above, shop.freeDeliveryAboveInr.asMoney())
                    } else {
                        stringResource(Res.string.shop_free_delivery_off)
                    },
                )
            }
        }
    }
    item(key = "pos_hours") {
        SectionCard(
            title = stringResource(Res.string.shop_hours_title),
            modifier = Modifier.fillMaxWidth(),
            action = editAction(canEdit, viewModel::startEditHours),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (shop.operatingHours.isEmpty()) SettingHint(stringResource(Res.string.shop_hours_not_set))
                shop.operatingHours.forEach { day ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(day.day, Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (day.closed) {
                                stringResource(Res.string.shop_closed)
                            } else {
                                stringResource(
                                    Res.string.shop_hours_range,
                                    formatClockTime(day.start) ?: day.start,
                                    formatClockTime(day.end) ?: day.end,
                                )
                            },
                            Modifier.weight(0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private fun editAction(canEdit: Boolean, onEdit: () -> Unit): (@Composable () -> Unit)? =
    if (canEdit) {
        { TextButton(onClick = onEdit) { Text(stringResource(Res.string.action_edit)) } }
    } else {
        null
    }

@Composable
private fun SettingHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ShopDetailsCard(shop: ShopDetailsDto, state: ShopProfileUiState, viewModel: ShopProfileViewModel, canEdit: Boolean) {
    val changePhoto = rememberImageSourceLauncher(ImageUploadFeature.SHOP_PHOTO, viewModel::changeShopPhoto)
    SectionCard(
        title = stringResource(Res.string.profile_shop_details),
        subtitle = shop.name,
        modifier = Modifier.fillMaxWidth(),
        action = if (canEdit) {
            { OutlinedButton(onClick = viewModel::startEditDetails) { Text(stringResource(Res.string.profile_edit_shop)) } }
        } else {
            null
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    NetworkImage(
                        url = shop.imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = stringResource(Res.string.profile_shop_photo),
                        localPreviewBytes = state.photoPreviewBytes,
                    )
                    if (state.uploadingPhoto) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                if (canEdit) {
                    OutlinedButton(onClick = changePhoto, enabled = !state.uploadingPhoto) {
                        Text(stringResource(Res.string.profile_change_photo))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.profile_field_location), style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (shop.lat == 0.0 && shop.lng == 0.0) stringResource(Res.string.profile_location_not_set)
                        else "${shop.lat.formatCoord()}, ${shop.lng.formatCoord()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (canEdit) {
                    TextButton(onClick = viewModel::requestUseCurrentLocation, enabled = !state.locating) {
                        Text(stringResource(if (state.locating) Res.string.profile_locating else Res.string.profile_use_current_location))
                    }
                }
            }
            HorizontalDivider()
            DetailRow(Res.string.profile_field_rating, stringResource(Res.string.profile_rating_value, shop.rating.formatOne(), shop.ratingCount))
            DetailRow(Res.string.profile_field_address, listOf(shop.address, shop.city, shop.state, shop.zip).filter { it.isNotBlank() }.joinToString(", "))
            DetailRow(
                Res.string.profile_field_shop_types,
                shop.shopTypes.ifEmpty { listOf(shop.shopType) }
                    .map { shopTypeName(it, state.shopTypeOptions) }
                    .joinToString(", "),
            )
            DetailRow(Res.string.profile_field_owner_name, shop.ownerName)
            DetailRow(Res.string.profile_field_owner_phone, shop.ownerPhone)
            DetailRow(Res.string.profile_field_owner_email, shop.ownerEmail)
            DetailRow(Res.string.profile_field_registration, shop.registrationNumber)
            DetailRow(Res.string.profile_field_tax, shop.taxId)
        }
    }
}

@Composable
private fun OrderRulesDialog(
    draft: OrderRulesDraft,
    onChange: ((OrderRulesDraft) -> OrderRulesDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormDialog(stringResource(Res.string.shop_order_rules_title), onSave, onDismiss) {
        OutlinedTextField(
            value = draft.minOrder,
            onValueChange = { v -> onChange { it.copy(minOrder = v) } },
            label = { Text(stringResource(Res.string.shop_min_order_label)) },
            supportingText = { Text(stringResource(Res.string.shop_min_order_hint)) },
            prefix = { Text(stringResource(Res.string.currency_symbol)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        ToggleRow(stringResource(Res.string.shop_free_delivery_label), draft.freeDeliveryEnabled, true) { on ->
            onChange { it.copy(freeDeliveryEnabled = on) }
        }
        if (draft.freeDeliveryEnabled) {
            OutlinedTextField(
                value = draft.freeDeliveryAbove,
                onValueChange = { v -> onChange { it.copy(freeDeliveryAbove = v) } },
                label = { Text(stringResource(Res.string.shop_free_delivery_amount)) },
                supportingText = { Text(stringResource(Res.string.shop_free_delivery_hint)) },
                prefix = { Text(stringResource(Res.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OperatingHoursDialog(
    days: List<ShopHoursDayDto>,
    onChange: (day: String, (ShopHoursDayDto) -> ShopHoursDayDto) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormDialog(stringResource(Res.string.shop_hours_title), onSave, onDismiss) {
        days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(day.day, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    FilterChip(
                        selected = day.closed,
                        onClick = { onChange(day.day) { it.copy(closed = !it.closed) } },
                        label = { Text(stringResource(Res.string.shop_closed)) },
                    )
                }
                if (!day.closed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePickerField(
                            label = stringResource(Res.string.master_hours_start),
                            value = day.start,
                            onChange = { v -> onChange(day.day) { it.copy(start = v) } },
                            modifier = Modifier.weight(1f),
                        )
                        TimePickerField(
                            label = stringResource(Res.string.master_hours_end),
                            value = day.end,
                            onChange = { v -> onChange(day.day) { it.copy(end = v) } },
                            modifier = Modifier.weight(1f),
                            defaultHour = DEFAULT_CLOSING_HOUR,
                        )
                    }
                }
            }
        }
    }
}

private const val DEFAULT_CLOSING_HOUR = 21

@Composable
private fun SubscriptionEntryCard(onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(Res.string.sub_profile_row),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun KeyboardCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SectionCard(
        title = stringResource(Res.string.keyboard_settings_title),
        subtitle = stringResource(Res.string.keyboard_floating_hint),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ToggleRow(stringResource(Res.string.keyboard_floating_toggle), enabled, true, onChange)
    }
}

/** Read-only: feature switches are owned by Pansari master. */
@Composable
private fun FeaturesCard(features: ShopFeaturesDto, voiceAutoAdd: Boolean, onVoiceAutoAddChange: (Boolean) -> Unit) {
    SectionCard(
        title = stringResource(Res.string.profile_features_title),
        subtitle = stringResource(Res.string.profile_features_master_managed),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ToggleRow(stringResource(Res.string.feature_barcode_scanner), features.barcodeScanner, false) {}
        ToggleRow(stringResource(Res.string.feature_voice_search), features.voiceSearch, false) {}
        if (features.voiceSearch) {
            Column(Modifier.padding(start = 16.dp)) {
                ToggleRow(stringResource(Res.string.voice_auto_add), voiceAutoAdd, true, onVoiceAutoAddChange)
                Text(
                    stringResource(Res.string.voice_auto_add_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ToggleRow(stringResource(Res.string.feature_barcode_search), features.barcodeSearch, false) {}
        ToggleRow(stringResource(Res.string.feature_report_generation), features.reportGeneration, false) {}
        ToggleRow(stringResource(Res.string.feature_online_orders), features.onlineOrders, false) {}
        ToggleRow(stringResource(Res.string.feature_inventory_alerts), features.inventoryAlerts, false) {}
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.staffItems(
    staff: List<ShopStaffDto>,
    access: ShopAccess,
    viewModel: ShopProfileViewModel,
) {
    item {
        SectionCard(
            title = stringResource(Res.string.staff_title),
            subtitle = stringResource(Res.string.staff_subtitle),
            modifier = Modifier.fillMaxWidth(),
            action = { Button(onClick = viewModel::startAddStaff) { Text(stringResource(Res.string.staff_add)) } },
        ) {
            if (staff.none { !it.isShopAdmin() }) {
                Text(stringResource(Res.string.staff_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    items(staff, key = { it.id }) { member ->
        val admin = member.isShopAdmin()
        val perms = ShopPermission.parse(member.permissions)
        val summary = when {
            admin -> stringResource(Res.string.staff_managed_by_master)
            ShopPermission.ADMIN in perms -> stringResource(Res.string.perm_admin)
            else -> ShopPermission.grantable.filter { it in perms }.map { stringResource(it.label()) }.joinToString(" · ")
        }
        SectionCard(
            title = member.displayName,
            subtitle = buildString {
                append(member.username)
                if (!member.active) append(" · ").append(stringResource(Res.string.staff_disabled))
            },
            modifier = Modifier.fillMaxWidth(),
            // Full-admin executives can only be changed by another admin (server enforces the same).
            action = if (admin || (ShopPermission.ADMIN in perms && !access.isAdmin)) {
                null
            } else {
                {
                    Row {
                        TextButton(onClick = { viewModel.startEditStaff(member) }) { Text(stringResource(Res.string.action_edit)) }
                        TextButton(onClick = { viewModel.requestDeleteStaff(member) }) { Text(stringResource(Res.string.action_remove)) }
                    }
                }
            },
        ) {
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.offerItems(
    offers: List<ShopOfferDto>,
    viewModel: ShopProfileViewModel,
) {
    item {
        SectionCard(
            title = stringResource(Res.string.offers_title),
            subtitle = stringResource(Res.string.offers_subtitle),
            modifier = Modifier.fillMaxWidth(),
            action = { Button(onClick = viewModel::startAddOffer) { Text(stringResource(Res.string.offers_add)) } },
        ) {
            if (offers.isEmpty()) Text(stringResource(Res.string.offers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(offers, key = { it.id }) { offer ->
        SectionCard(
            title = offer.title,
            subtitle = offerSummary(offer),
            modifier = Modifier.fillMaxWidth(),
            action = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = offer.active, onCheckedChange = { viewModel.setOfferActive(offer, it) })
                    TextButton(onClick = { viewModel.startEditOffer(offer) }) { Text(stringResource(Res.string.action_edit)) }
                    TextButton(onClick = { viewModel.requestDeleteOffer(offer) }) { Text(stringResource(Res.string.action_remove)) }
                }
            },
        ) {
            if (offer.description.isNotBlank()) Text(offer.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun offerSummary(offer: ShopOfferDto): String {
    val amount = if (offer.discountType == OfferDiscountType.FLAT) {
        stringResource(Res.string.offer_summary_flat, offer.value.asMoney())
    } else {
        stringResource(Res.string.offer_summary_percent, offer.value.formatOne())
    }
    val scope = if (offer.scope == OfferScope.PRODUCT) {
        stringResource(Res.string.offer_summary_products, offer.productIds.size)
    } else if (offer.minBillAmount > 0.0) {
        stringResource(Res.string.offer_summary_min_bill, offer.minBillAmount.asMoney())
    } else {
        stringResource(Res.string.offer_scope_bill)
    }
    return "$amount · $scope"
}

private fun androidx.compose.foundation.lazy.LazyListScope.ratingItems(
    shop: ShopDetailsDto,
    ratings: List<ShopReview>,
    viewModel: ShopProfileViewModel,
) {
    item {
        SectionCard(
            title = stringResource(Res.string.ratings_title),
            subtitle = stringResource(Res.string.profile_rating_value, shop.rating.formatOne(), shop.ratingCount),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ratings.isEmpty()) Text(stringResource(Res.string.ratings_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(ratings, key = { it.id }) { review ->
        SectionCard(
            title = "${"★".repeat(review.stars)}${"☆".repeat(5 - review.stars)}  ${review.customerName}",
            subtitle = formatPrintDateTime(review.createdAtEpochMs),
            modifier = Modifier.fillMaxWidth(),
            action = {
                TextButton(onClick = { viewModel.startReply(review) }) {
                    Text(stringResource(if (review.reply == null) Res.string.ratings_reply else Res.string.ratings_edit_reply))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                review.comment?.takeIf { it.isNotBlank() }?.let { Text(it) }
                review.reply?.let {
                    Text(
                        stringResource(Res.string.ratings_shop_reply, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitorsCard(state: ShopProfileUiState, onRange: (VisitorRange) -> Unit) {
    SectionCard(
        title = stringResource(Res.string.visitors_title),
        subtitle = stringResource(Res.string.visitors_subtitle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisitorRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.visitorRange == range,
                        onClick = { onRange(range) },
                        label = { Text(stringResource(range.label())) },
                    )
                }
            }
            val stats = state.visitors ?: return@Column
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(Res.string.visitors_unique), stats.uniqueVisitors.toString(), Modifier.weight(1f))
                StatTile(stringResource(Res.string.visitors_visits), stats.visitDays.toString(), Modifier.weight(1f))
            }
            if (stats.daily.isNotEmpty()) {
                Text(stringResource(Res.string.visitors_daily_title), style = MaterialTheme.typography.labelLarge)
                val max = stats.daily.maxOf { it.value }.coerceAtLeast(1.0)
                stats.daily.takeLast(MAX_DAILY_ROWS).forEach { point ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(point.label, Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (point.value / max).toFloat() }, modifier = Modifier.weight(1f))
                        Text(point.value.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private const val MAX_DAILY_ROWS = 31

private fun VisitorRange.label(): StringResource = when (this) {
    VisitorRange.TODAY -> Res.string.visitors_range_today
    VisitorRange.WEEK -> Res.string.visitors_range_week
    VisitorRange.MONTH -> Res.string.visitors_range_month
    VisitorRange.ALL -> Res.string.visitors_range_all
}

@Composable
private fun ShopDetailsDialog(
    draft: ShopDetailsDraft,
    shopTypeOptions: List<ShopTypeOptionDto>,
    onChange: ((ShopDetailsDraft) -> ShopDetailsDraft) -> Unit,
    onToggleShopType: (String) -> Unit,
    onMakePrimary: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormDialog(stringResource(Res.string.profile_edit_shop), onSave, onDismiss) {
        Field(Res.string.profile_field_name, draft.name) { v -> onChange { it.copy(name = v) } }
        ShopTypesEditor(draft.shopTypes, shopTypeOptions, onToggleShopType, onMakePrimary)
        Field(Res.string.profile_field_address, draft.address) { v -> onChange { it.copy(address = v) } }
        Field(Res.string.profile_field_city, draft.city) { v -> onChange { it.copy(city = v) } }
        Field(Res.string.profile_field_state, draft.state) { v -> onChange { it.copy(state = v) } }
        Field(Res.string.profile_field_zip, draft.zip, KeyboardType.Number) { v -> onChange { it.copy(zip = v) } }
        Field(Res.string.profile_field_owner_name, draft.ownerName) { v -> onChange { it.copy(ownerName = v) } }
        Field(Res.string.profile_field_owner_phone, draft.ownerPhone, KeyboardType.Phone) { v -> onChange { it.copy(ownerPhone = v) } }
        Field(Res.string.profile_field_owner_email, draft.ownerEmail, KeyboardType.Email) { v -> onChange { it.copy(ownerEmail = v) } }
        Field(Res.string.profile_field_radius, draft.deliveryRadiusKm, KeyboardType.Decimal) { v -> onChange { it.copy(deliveryRadiusKm = v) } }
        DeliveryUntilField(draft.deliveryUntil) { v -> onChange { it.copy(deliveryUntil = v) } }
        Field(Res.string.profile_field_discount, draft.discountPercent, KeyboardType.Decimal) { v -> onChange { it.copy(discountPercent = v) } }
        Field(Res.string.profile_field_registration, draft.registrationNumber) { v -> onChange { it.copy(registrationNumber = v) } }
        Field(Res.string.profile_field_tax, draft.taxId) { v -> onChange { it.copy(taxId = v) } }
    }
}

@Composable
private fun shopTypeName(id: String, options: List<ShopTypeOptionDto>): String =
    options.firstOrNull { it.id == id }?.name
        ?: ShopType.entries.firstOrNull { it.apiValue == id }?.let { shopTypeLabel(it) }
        ?: id.replace('_', ' ')

/** Multi-select; the first selected type is primary (switchable in the second row). */
@Composable
private fun ShopTypesEditor(
    selected: List<String>,
    options: List<ShopTypeOptionDto>,
    onToggle: (String) -> Unit,
    onMakePrimary: (String) -> Unit,
) {
    val ids = (options.map { it.id } + selected).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.profile_field_shop_types), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(Res.string.shop_types_select_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ids.forEach { id ->
                FilterChip(
                    selected = id in selected,
                    onClick = { onToggle(id) },
                    label = { Text(shopTypeName(id, options)) },
                )
            }
        }
        val primary = selected.firstOrNull()
        if (selected.size > 1 && primary != null) {
            Text(stringResource(Res.string.shop_types_primary_label), style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.forEach { id ->
                    FilterChip(
                        selected = id == primary,
                        onClick = { onMakePrimary(id) },
                        label = { Text(shopTypeName(id, options)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffDialog(
    draft: StaffDraft,
    access: ShopAccess,
    onChange: ((StaffDraft) -> StaffDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = draft.id == null
    val canSave = !isNew || draft.usernameStatus !in setOf(UsernameStatus.TAKEN, UsernameStatus.CHECKING)
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(if (isNew) Res.string.staff_add else Res.string.staff_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            StaffForm(draft, access, isNew, onChange)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
                Button(onClick = onSave, enabled = canSave) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}

@Composable
private fun StaffForm(
    draft: StaffDraft,
    access: ShopAccess,
    isNew: Boolean,
    onChange: ((StaffDraft) -> StaffDraft) -> Unit,
) {
    Field(Res.string.staff_field_name, draft.displayName) { v -> onChange { it.copy(displayName = v) } }
    val usernameHint = when (draft.usernameStatus) {
        UsernameStatus.CHECKING -> Res.string.staff_username_checking
        UsernameStatus.AVAILABLE -> Res.string.staff_username_available
        UsernameStatus.TAKEN -> Res.string.staff_username_taken
        UsernameStatus.ERROR -> Res.string.staff_username_check_failed
        UsernameStatus.IDLE -> null
    }
    OutlinedTextField(
        value = draft.username,
        onValueChange = { v -> onChange { it.copy(username = v.trim().lowercase()) } },
        label = { Text(stringResource(Res.string.staff_field_username)) },
        enabled = isNew,
        singleLine = true,
        isError = isNew && draft.usernameStatus == UsernameStatus.TAKEN,
        supportingText = if (isNew && usernameHint != null) {
            {
                Text(
                    stringResource(usernameHint),
                    color = if (draft.usernameStatus == UsernameStatus.AVAILABLE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Unspecified
                    },
                )
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = draft.password,
        onValueChange = { v -> onChange { it.copy(password = v) } },
        label = { Text(stringResource(if (isNew) Res.string.staff_field_password else Res.string.staff_field_password_keep)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    ToggleRow(stringResource(Res.string.staff_active), draft.active, true) { v -> onChange { it.copy(active = v) } }
    HorizontalDivider()
    Text(stringResource(Res.string.staff_access_title), style = MaterialTheme.typography.labelLarge)
    val fullAdmin = ShopPermission.ADMIN in draft.permissions
    CheckRow(stringResource(ShopPermission.ADMIN.label()), fullAdmin, enabled = access.isAdmin) { checked ->
        onChange { it.copy(permissions = if (checked) setOf(ShopPermission.ADMIN) else setOf(ShopPermission.BILLING)) }
    }
    ShopPermission.grantable.forEach { perm ->
        CheckRow(
            stringResource(perm.label()),
            fullAdmin || perm in draft.permissions,
            enabled = !fullAdmin && access.can(perm),
        ) { checked ->
            onChange { it.copy(permissions = if (checked) it.permissions + perm else it.permissions - perm) }
        }
    }
}

@Composable
private fun OfferDialog(
    draft: OfferDraft,
    products: List<org.bhargav.pansariwala.domain.model.Product>,
    onChange: ((OfferDraft) -> OfferDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormDialog(stringResource(if (draft.id == null) Res.string.offers_add else Res.string.offers_edit), onSave, onDismiss) {
        Field(Res.string.offer_field_title, draft.title) { v -> onChange { it.copy(title = v) } }
        Field(Res.string.offer_field_description, draft.description) { v -> onChange { it.copy(description = v) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(draft.scope == OfferScope.BILL, { onChange { it.copy(scope = OfferScope.BILL) } }, { Text(stringResource(Res.string.offer_scope_bill)) })
            FilterChip(draft.scope == OfferScope.PRODUCT, { onChange { it.copy(scope = OfferScope.PRODUCT) } }, { Text(stringResource(Res.string.offer_scope_product)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(draft.discountType == OfferDiscountType.PERCENT, { onChange { it.copy(discountType = OfferDiscountType.PERCENT) } }, { Text(stringResource(Res.string.offer_type_percent)) })
            FilterChip(draft.discountType == OfferDiscountType.FLAT, { onChange { it.copy(discountType = OfferDiscountType.FLAT) } }, { Text(stringResource(Res.string.offer_type_flat)) })
        }
        Field(Res.string.offer_field_value, draft.value, KeyboardType.Decimal) { v -> onChange { it.copy(value = v) } }
        if (draft.scope == OfferScope.BILL) {
            Field(Res.string.offer_field_min_bill, draft.minBillAmount, KeyboardType.Decimal) { v -> onChange { it.copy(minBillAmount = v) } }
        } else {
            Text(stringResource(Res.string.offer_products_title, draft.productIds.size), style = MaterialTheme.typography.labelLarge)
            Field(Res.string.offer_products_search, draft.productQuery) { v -> onChange { it.copy(productQuery = v) } }
            val query = draft.productQuery.trim().lowercase()
            products
                .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.nameHi.contains(query) || it.translations.values.any { t -> t.contains(query, ignoreCase = true) } }
                .sortedByDescending { it.id in draft.productIds }
                .take(MAX_PRODUCT_ROWS)
                .forEach { product ->
                    CheckRow("${product.localizedName} · ${product.sellingPrice.asMoney()}", product.id in draft.productIds) { checked ->
                        onChange { it.copy(productIds = if (checked) it.productIds + product.id else it.productIds - product.id) }
                    }
                }
        }
        ToggleRow(stringResource(Res.string.offer_active), draft.active, true) { v -> onChange { it.copy(active = v) } }
    }
}

private const val MAX_PRODUCT_ROWS = 40

@Composable
private fun FormDialog(
    title: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(Res.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.action_yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_no)) } },
    )
}

@Composable
private fun Field(
    label: StringResource,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeliveryUntilField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TimePickerField(
            label = stringResource(Res.string.shop_delivery_until_label),
            value = value,
            onChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            emptyText = stringResource(Res.string.shop_delivery_until_none),
            clearLabel = stringResource(Res.string.shop_delivery_until_clear),
            defaultHour = DEFAULT_DELIVERY_CUTOFF_HOUR,
        )
        Text(
            stringResource(Res.string.shop_delivery_until_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val DEFAULT_DELIVERY_CUTOFF_HOUR = 17

@Composable
private fun DetailRow(label: StringResource, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(label),
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Double.formatCoord(): String = (kotlin.math.round(this * 100_000) / 100_000).toString()

private fun Double.formatOne(): String {
    val tenths = kotlin.math.round(this * 10).toLong()
    return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpiEditSheet(
    draft: String,
    otp: OtpStepState,
    onDraft: (String) -> Unit,
    onSendOtp: () -> Unit,
    onCode: (String) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(Res.string.upi_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.upi_sheet_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                label = { Text(stringResource(Res.string.profile_field_upi)) },
                singleLine = true,
                enabled = !otp.codeSent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (otp.codeSent) {
                OtpStepSection(
                    state = otp,
                    submitLabel = stringResource(Res.string.upi_verify_save),
                    onCode = onCode,
                    onResend = onSendOtp,
                    onSubmit = onVerify,
                )
            } else {
                otp.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = onSendOtp, enabled = !otp.busy && draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.action_send_otp))
                }
            }
        }
    }
}
