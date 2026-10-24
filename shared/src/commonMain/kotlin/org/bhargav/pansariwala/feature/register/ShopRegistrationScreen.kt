package org.bhargav.pansariwala.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.bhargav.pansariwala.api.ShopHoursDayDto
import org.bhargav.pansariwala.designsystem.OtpCodeField
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PermissionRationaleSheet
import org.bhargav.pansariwala.designsystem.ResendOtpButton
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.TimePickerField
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.mapLocationPickerSupported
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_hide
import pansariwala.shared.generated.resources.action_show
import pansariwala.shared.generated.resources.location_rationale_allow
import pansariwala.shared.generated.resources.location_rationale_footer
import pansariwala.shared.generated.resources.location_rationale_title
import pansariwala.shared.generated.resources.master_hours_end
import pansariwala.shared.generated.resources.master_hours_start
import pansariwala.shared.generated.resources.place_search_searching
import pansariwala.shared.generated.resources.reg_address
import pansariwala.shared.generated.resources.reg_back
import pansariwala.shared.generated.resources.reg_city
import pansariwala.shared.generated.resources.reg_confirm_password
import pansariwala.shared.generated.resources.reg_country
import pansariwala.shared.generated.resources.reg_credentials_subtitle
import pansariwala.shared.generated.resources.reg_credentials_title
import pansariwala.shared.generated.resources.reg_business_subtitle
import pansariwala.shared.generated.resources.reg_business_title
import pansariwala.shared.generated.resources.reg_edit
import pansariwala.shared.generated.resources.reg_hours
import pansariwala.shared.generated.resources.reg_location_permission_denied
import pansariwala.shared.generated.resources.reg_location_rationale
import pansariwala.shared.generated.resources.reg_location_rationale_point_pin
import pansariwala.shared.generated.resources.reg_location_rationale_point_verify
import pansariwala.shared.generated.resources.reg_location_search
import pansariwala.shared.generated.resources.reg_location_subtitle
import pansariwala.shared.generated.resources.reg_location_title
import pansariwala.shared.generated.resources.reg_location_verified
import pansariwala.shared.generated.resources.reg_location_not_verified
import pansariwala.shared.generated.resources.reg_location_pick_on_map
import pansariwala.shared.generated.resources.reg_location_verify
import pansariwala.shared.generated.resources.reg_next
import pansariwala.shared.generated.resources.reg_not_provided
import pansariwala.shared.generated.resources.reg_owner_email
import pansariwala.shared.generated.resources.reg_owner_name
import pansariwala.shared.generated.resources.reg_owner_phone
import pansariwala.shared.generated.resources.reg_owner_subtitle
import pansariwala.shared.generated.resources.reg_owner_title
import pansariwala.shared.generated.resources.reg_password
import pansariwala.shared.generated.resources.reg_password_hint
import pansariwala.shared.generated.resources.reg_phone_label
import pansariwala.shared.generated.resources.reg_phone_subtitle
import pansariwala.shared.generated.resources.reg_phone_title
import pansariwala.shared.generated.resources.reg_registration_number
import pansariwala.shared.generated.resources.reg_review_subtitle
import pansariwala.shared.generated.resources.reg_review_title
import pansariwala.shared.generated.resources.reg_send_otp
import pansariwala.shared.generated.resources.reg_change_number
import pansariwala.shared.generated.resources.reg_shop_name
import pansariwala.shared.generated.resources.reg_shop_subtitle
import pansariwala.shared.generated.resources.reg_shop_title
import pansariwala.shared.generated.resources.reg_shop_types
import pansariwala.shared.generated.resources.reg_state
import pansariwala.shared.generated.resources.reg_step_business
import pansariwala.shared.generated.resources.reg_step_credentials
import pansariwala.shared.generated.resources.reg_step_location
import pansariwala.shared.generated.resources.reg_step_owner
import pansariwala.shared.generated.resources.reg_step_phone
import pansariwala.shared.generated.resources.reg_step_progress
import pansariwala.shared.generated.resources.reg_step_review
import pansariwala.shared.generated.resources.reg_step_shop
import pansariwala.shared.generated.resources.reg_submit
import pansariwala.shared.generated.resources.reg_tax_id
import pansariwala.shared.generated.resources.reg_title
import pansariwala.shared.generated.resources.reg_username
import pansariwala.shared.generated.resources.reg_username_available
import pansariwala.shared.generated.resources.reg_username_checking
import pansariwala.shared.generated.resources.reg_username_taken
import pansariwala.shared.generated.resources.reg_verify_otp
import pansariwala.shared.generated.resources.reg_zip
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.weekday_friday
import pansariwala.shared.generated.resources.weekday_monday
import pansariwala.shared.generated.resources.weekday_saturday
import pansariwala.shared.generated.resources.weekday_sunday
import pansariwala.shared.generated.resources.weekday_thursday
import pansariwala.shared.generated.resources.weekday_tuesday
import pansariwala.shared.generated.resources.weekday_wednesday

/** Self-service shop owner onboarding: phone OTP first, then stepped details saved as a resumable draft. */
@Composable
fun ShopRegistrationScreen(
    onBack: () -> Unit,
    onRegistered: (shopName: String, loginUsername: String) -> Unit,
    viewModel: ShopRegistrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val goBack = { if (!viewModel.back()) onBack() }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = state.step != RegistrationStep.PHONE || state.otpSent,
        onBackCompleted = { goBack() },
    )
    LocationPermissionUi(state, viewModel)
    PansariScreen(
        title = stringResource(Res.string.reg_title),
        onBack = goBack,
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= WIDE_LAYOUT_MIN
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    StepRail(
                        state = state,
                        onOpen = viewModel::openStep,
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(24.dp),
                    )
                    StepPane(state, viewModel, onRegistered, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    StepProgress(state)
                    StepPane(state, viewModel, onRegistered, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

private val WIDE_LAYOUT_MIN = 840.dp

@Composable
private fun LocationPermissionUi(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    if (state.showLocationRationale) {
        PermissionRationaleSheet(
            icon = Icons.Default.LocationOn,
            title = stringResource(Res.string.location_rationale_title),
            message = stringResource(Res.string.reg_location_rationale),
            points = listOf(
                stringResource(Res.string.reg_location_rationale_point_verify),
                stringResource(Res.string.reg_location_rationale_point_pin),
            ),
            footer = stringResource(Res.string.location_rationale_footer),
            confirmLabel = stringResource(Res.string.location_rationale_allow),
            onConfirm = viewModel::acceptLocationRationale,
            onDismiss = viewModel::dismissLocationRationale,
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
        message = stringResource(Res.string.reg_location_permission_denied),
    )
}

private fun RegistrationStep.label(): StringResource = when (this) {
    RegistrationStep.PHONE -> Res.string.reg_step_phone
    RegistrationStep.OWNER -> Res.string.reg_step_owner
    RegistrationStep.SHOP -> Res.string.reg_step_shop
    RegistrationStep.LOCATION -> Res.string.reg_step_location
    RegistrationStep.BUSINESS -> Res.string.reg_step_business
    RegistrationStep.CREDENTIALS -> Res.string.reg_step_credentials
    RegistrationStep.REVIEW -> Res.string.reg_step_review
}

@Composable
private fun StepProgress(state: ShopRegistrationUiState) {
    val total = RegistrationStep.entries.size
    val index = state.step.ordinal + 1
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            stringResource(Res.string.reg_step_progress, index, total) + " · " + stringResource(state.step.label()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { index.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun StepRail(
    state: ShopRegistrationUiState,
    onOpen: (RegistrationStep) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RegistrationStep.entries.forEach { step ->
            val done = step < state.reachedStep || (step == RegistrationStep.PHONE && state.step > step)
            val current = step == state.step
            val reachable = step != RegistrationStep.PHONE && step <= state.reachedStep
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable(enabled = reachable && !current) { onOpen(step) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                current || done -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done && !current) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            (step.ordinal + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    stringResource(step.label()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun StepPane(
    state: ShopRegistrationUiState,
    viewModel: ShopRegistrationViewModel,
    onRegistered: (String, String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.step) {
                RegistrationStep.PHONE -> PhoneStep(state, viewModel)
                RegistrationStep.OWNER -> OwnerStep(state, viewModel)
                RegistrationStep.SHOP -> ShopStep(state, viewModel)
                RegistrationStep.LOCATION -> LocationStep(state, viewModel)
                RegistrationStep.BUSINESS -> BusinessStep(state, viewModel)
                RegistrationStep.CREDENTIALS -> CredentialsStep(state, viewModel)
                RegistrationStep.REVIEW -> ReviewStep(state, viewModel)
            }
            if (state.step != RegistrationStep.PHONE) {
                StepButtons(state, viewModel, onRegistered)
            }
        }
    }
}

@Composable
private fun StepHeader(title: StringResource, subtitle: String) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StepButtons(
    state: ShopRegistrationUiState,
    viewModel: ShopRegistrationViewModel,
    onRegistered: (String, String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.step > RegistrationStep.OWNER || state.editingFromReview) {
            OutlinedButton(
                onClick = { viewModel.back() },
                enabled = !state.loading,
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(stringResource(Res.string.reg_back)) }
        }
        Button(
            onClick = {
                if (state.step == RegistrationStep.REVIEW) viewModel.submit(onRegistered) else viewModel.next()
            },
            enabled = !state.loading,
            modifier = Modifier.weight(1f).height(52.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(if (state.step == RegistrationStep.REVIEW) Res.string.reg_submit else Res.string.reg_next))
            }
        }
    }
}

@Composable
private fun PhoneStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_phone_title, stringResource(Res.string.reg_phone_subtitle))
    OutlinedTextField(
        value = state.phone,
        onValueChange = viewModel::setPhone,
        label = { Text(stringResource(Res.string.reg_phone_label)) },
        prefix = { Text(AppConstants.DEFAULT_PHONE_COUNTRY_CODE + " ") },
        enabled = !state.otpSent && !state.loading,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    if (!state.otpSent) {
        Button(
            onClick = viewModel::sendOtp,
            enabled = !state.loading && state.phone.length == AppConstants.PHONE_LOCAL_DIGITS,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(Res.string.reg_send_otp)) }
        return
    }
    state.hint?.let {
        Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    OtpCodeField(
        value = state.otp,
        onValueChange = viewModel::setOtp,
        enabled = !state.loading,
        onDone = viewModel::verifyOtp,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { viewModel.back() }, enabled = !state.loading) {
            Text(stringResource(Res.string.reg_change_number))
        }
        ResendOtpButton(state.resendInSec, state.loading, viewModel::sendOtp)
    }
    Button(
        onClick = viewModel::verifyOtp,
        enabled = !state.loading && state.otp.length == AppConstants.OTP_LENGTH,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(stringResource(Res.string.reg_verify_otp)) }
}

@Composable
private fun OwnerStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_owner_title, stringResource(Res.string.reg_owner_subtitle))
    Text(
        stringResource(Res.string.reg_owner_phone, AppConstants.DEFAULT_PHONE_COUNTRY_CODE + " " + state.draft.phone),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    RegField(state.draft.ownerName, Res.string.reg_owner_name) { v -> viewModel.updateDraft { it.copy(ownerName = v) } }
    RegField(state.draft.ownerEmail, Res.string.reg_owner_email, KeyboardType.Email) { v ->
        viewModel.updateDraft { it.copy(ownerEmail = v.trim()) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_shop_title, stringResource(Res.string.reg_shop_subtitle))
    RegField(state.draft.shopName, Res.string.reg_shop_name) { v -> viewModel.updateDraft { it.copy(shopName = v) } }
    if (state.shopTypeOptions.isNotEmpty()) {
        Text(stringResource(Res.string.reg_shop_types), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.shopTypeOptions.forEach { type ->
                FilterChip(
                    selected = type.id in state.draft.shopTypes,
                    onClick = { viewModel.toggleShopType(type.id) },
                    label = { Text(type.name) },
                )
            }
        }
    }
}

@Composable
private fun LocationStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(
        Res.string.reg_location_title,
        stringResource(Res.string.reg_location_subtitle, AppConstants.ShopRegistration.LOCATION_TOLERANCE_KM),
    )
    OutlinedButton(
        onClick = viewModel::useCurrentLocation,
        enabled = !state.loading && !state.locating,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (state.locating) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.reg_location_verify))
    }
    if (mapLocationPickerSupported) {
        OutlinedButton(
            onClick = viewModel::pickOnMap,
            enabled = !state.loading && !state.pickingOnMap,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.pickingOnMap) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.reg_location_pick_on_map))
        }
    }
    val verified = state.locationVerified
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val tint = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Icon(if (verified) Icons.Default.CheckCircle else Icons.Default.Warning, null, Modifier.size(18.dp), tint = tint)
        Text(
            stringResource(if (verified) Res.string.reg_location_verified else Res.string.reg_location_not_verified),
            style = MaterialTheme.typography.bodySmall,
            color = tint,
        )
    }
    OutlinedTextField(
        value = state.placeQuery,
        onValueChange = viewModel::setPlaceQuery,
        label = { Text(stringResource(Res.string.reg_location_search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        supportingText = if (state.searchingPlaces) {
            { Text(stringResource(Res.string.place_search_searching)) }
        } else {
            null
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.predictions.isNotEmpty()) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            state.predictions.forEachIndexed { index, prediction ->
                if (index > 0) HorizontalDivider()
                ListItem(
                    headlineContent = { Text(prediction.description, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { viewModel.selectPlace(prediction.placeId) },
                )
            }
        }
    }
    val d = state.draft
    RegField(d.address, Res.string.reg_address, minLines = 2) { v -> viewModel.updateDraft { it.copy(address = v) } }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RegField(d.city, Res.string.reg_city, modifier = Modifier.weight(1f)) { v -> viewModel.updateDraft { it.copy(city = v) } }
        RegField(d.state, Res.string.reg_state, modifier = Modifier.weight(1f)) { v -> viewModel.updateDraft { it.copy(state = v) } }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RegField(d.zip, Res.string.reg_zip, KeyboardType.Number, modifier = Modifier.weight(1f)) { v ->
            viewModel.updateDraft { it.copy(zip = v.filter(Char::isDigit).take(AppConstants.ShopRegistration.ZIP_LENGTH)) }
        }
        OutlinedTextField(
            value = d.country,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.reg_country)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BusinessStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_business_title, stringResource(Res.string.reg_business_subtitle))
    val d = state.draft
    RegField(d.registrationNumber, Res.string.reg_registration_number) { v ->
        viewModel.updateDraft { it.copy(registrationNumber = v) }
    }
    RegField(d.taxId, Res.string.reg_tax_id) { v -> viewModel.updateDraft { it.copy(taxId = v.uppercase()) } }
    Text(stringResource(Res.string.reg_hours), style = MaterialTheme.typography.titleSmall)
    d.operatingHours.forEach { day -> HoursRow(day, viewModel) }
}

@Composable
private fun HoursRow(day: ShopHoursDayDto, viewModel: ShopRegistrationViewModel) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(weekdayLabel(day.day), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                FilterChip(
                    selected = day.closed,
                    onClick = { viewModel.updateHours(day.day) { it.copy(closed = !it.closed) } },
                    label = { Text(stringResource(Res.string.shop_closed)) },
                )
            }
            if (!day.closed) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimePickerField(
                        label = stringResource(Res.string.master_hours_start),
                        value = day.start,
                        onChange = { v -> viewModel.updateHours(day.day) { it.copy(start = v) } },
                        modifier = Modifier.weight(1f),
                    )
                    TimePickerField(
                        label = stringResource(Res.string.master_hours_end),
                        value = day.end,
                        onChange = { v -> viewModel.updateHours(day.day) { it.copy(end = v) } },
                        modifier = Modifier.weight(1f),
                        defaultHour = DEFAULT_CLOSING_HOUR,
                    )
                }
            }
        }
    }
}

private const val DEFAULT_CLOSING_HOUR = 21

@Composable
private fun weekdayLabel(day: String): String = when (AppConstants.ShopRegistration.WEEK_DAYS.indexOf(day)) {
    0 -> stringResource(Res.string.weekday_monday)
    1 -> stringResource(Res.string.weekday_tuesday)
    2 -> stringResource(Res.string.weekday_wednesday)
    3 -> stringResource(Res.string.weekday_thursday)
    4 -> stringResource(Res.string.weekday_friday)
    5 -> stringResource(Res.string.weekday_saturday)
    6 -> stringResource(Res.string.weekday_sunday)
    else -> day
}

@Composable
private fun CredentialsStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_credentials_title, stringResource(Res.string.reg_credentials_subtitle))
    OutlinedTextField(
        value = state.draft.loginUsername,
        onValueChange = viewModel::setUsername,
        label = { Text(stringResource(Res.string.reg_username)) },
        singleLine = true,
        isError = state.usernameStatus == UsernameStatus.TAKEN,
        supportingText = when (state.usernameStatus) {
            UsernameStatus.CHECKING -> { { Text(stringResource(Res.string.reg_username_checking)) } }
            UsernameStatus.AVAILABLE -> {
                { Text(stringResource(Res.string.reg_username_available), color = MaterialTheme.colorScheme.primary) }
            }
            UsernameStatus.TAKEN -> { { Text(stringResource(Res.string.reg_username_taken)) } }
            null -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    var visible by remember { mutableStateOf(false) }
    val transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::setPassword,
        label = { Text(stringResource(Res.string.reg_password)) },
        singleLine = true,
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) Res.string.action_hide else Res.string.action_show))
            }
        },
        supportingText = {
            Text(stringResource(Res.string.reg_password_hint, AppConstants.ShopRegistration.MIN_PASSWORD))
        },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.confirmPassword,
        onValueChange = viewModel::setConfirmPassword,
        label = { Text(stringResource(Res.string.reg_confirm_password)) },
        singleLine = true,
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = state.confirmPassword.isNotEmpty() && state.confirmPassword != state.password,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReviewStep(state: ShopRegistrationUiState, viewModel: ShopRegistrationViewModel) {
    StepHeader(Res.string.reg_review_title, stringResource(Res.string.reg_review_subtitle))
    val d = state.draft
    val typeNames = state.shopTypeOptions.filter { it.id in d.shopTypes }.joinToString { it.name }
    ReviewCard(RegistrationStep.OWNER, viewModel, d.ownerName, d.ownerEmail, AppConstants.DEFAULT_PHONE_COUNTRY_CODE + " " + d.phone)
    ReviewCard(RegistrationStep.SHOP, viewModel, d.shopName, typeNames)
    ReviewCard(
        RegistrationStep.LOCATION,
        viewModel,
        d.address,
        listOf(d.city, d.state, d.zip).filter { it.isNotBlank() }.joinToString(", "),
        stringResource(if (state.locationVerified) Res.string.reg_location_verified else Res.string.reg_location_not_verified),
    )
    val hourLines = d.operatingHours.filterNot { it.closed }.map { day ->
        "${weekdayLabel(day.day)}: ${day.start} – ${day.end}"
    }
    ReviewCard(
        RegistrationStep.BUSINESS,
        viewModel,
        d.registrationNumber,
        d.taxId,
        hourLines.joinToString("\n"),
    )
    ReviewCard(RegistrationStep.CREDENTIALS, viewModel, d.loginUsername)
}

@Composable
private fun ReviewCard(step: RegistrationStep, viewModel: ShopRegistrationViewModel, vararg lines: String) {
    SectionCard(
        title = stringResource(step.label()),
        action = { TextButton(onClick = { viewModel.openStep(step) }) { Text(stringResource(Res.string.reg_edit)) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val notProvided = stringResource(Res.string.reg_not_provided)
            lines.forEach { line ->
                Text(
                    line.ifBlank { notProvided },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (line.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RegField(
    value: String,
    label: StringResource,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = minLines == 1,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}
