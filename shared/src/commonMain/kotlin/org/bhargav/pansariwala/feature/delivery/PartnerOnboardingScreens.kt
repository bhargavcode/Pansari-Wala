package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.OnboardingFormCard
import org.bhargav.pansariwala.designsystem.OnboardingHero
import org.bhargav.pansariwala.designsystem.OnboardingHighlight
import org.bhargav.pansariwala.designsystem.OtpCodeField
import org.bhargav.pansariwala.designsystem.PansariLinkButton
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.ResendOtpButton
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.PhotoUploadUi
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.OtpAutofillEffect
import org.bhargav.pansariwala.platform.PhoneNumberHintEffect
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_continue
import pansariwala.shared.generated.resources.action_use_current_location
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_email
import pansariwala.shared.generated.resources.field_locality
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.field_place_search
import pansariwala.shared.generated.resources.field_vehicle_photo
import pansariwala.shared.generated.resources.field_vehicle_reg
import pansariwala.shared.generated.resources.hint_address_pick_place
import pansariwala.shared.generated.resources.hint_vehicle_photo
import pansariwala.shared.generated.resources.onboarding_otp_heading
import pansariwala.shared.generated.resources.onboarding_phone_heading
import pansariwala.shared.generated.resources.onboarding_phone_subheading
import pansariwala.shared.generated.resources.onboarding_step_of
import pansariwala.shared.generated.resources.partner_action_login
import pansariwala.shared.generated.resources.partner_action_sign_up
import pansariwala.shared.generated.resources.partner_doc_dl
import pansariwala.shared.generated.resources.partner_doc_id
import pansariwala.shared.generated.resources.partner_login_prompt
import pansariwala.shared.generated.resources.partner_onboarding_highlight_earn
import pansariwala.shared.generated.resources.partner_onboarding_highlight_flexible
import pansariwala.shared.generated.resources.partner_onboarding_highlight_nearby
import pansariwala.shared.generated.resources.partner_onboarding_tagline
import pansariwala.shared.generated.resources.partner_onboarding_title
import pansariwala.shared.generated.resources.partner_profile_pic
import pansariwala.shared.generated.resources.partner_register_step_address
import pansariwala.shared.generated.resources.partner_register_step_details
import pansariwala.shared.generated.resources.partner_register_step_vehicle
import pansariwala.shared.generated.resources.partner_register_title
import pansariwala.shared.generated.resources.partner_signup_join
import pansariwala.shared.generated.resources.partner_verification_status

private const val REGISTER_FORM_STEPS = 3

@Composable
fun PartnerLoginScreen(
    onOtpSent: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: PartnerLoginViewModel,
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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingHero(
                title = stringResource(Res.string.partner_onboarding_title),
                tagline = stringResource(Res.string.partner_onboarding_tagline),
                heroEmoji = "🛵",
                floatingEmojis = listOf("📦", "📍", "💰", "🛍️", "⏱️", "🏪"),
                highlights = listOf(
                    OnboardingHighlight("💰", stringResource(Res.string.partner_onboarding_highlight_earn)),
                    OnboardingHighlight("⏰", stringResource(Res.string.partner_onboarding_highlight_flexible)),
                    OnboardingHighlight("📍", stringResource(Res.string.partner_onboarding_highlight_nearby)),
                ),
                startColor = MaterialTheme.colorScheme.primary,
                endColor = lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.35f),
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
                PhoneNumberHintEffect(state.phone, viewModel::setPhone)
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::setPhone,
                    label = { Text(stringResource(Res.string.field_phone)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                PartnerPrimaryButton(
                    text = stringResource(Res.string.partner_action_login),
                    onClick = { viewModel.sendOtp(onOtpSent) },
                    enabled = !state.loading && state.phone.length >= 10,
                )
                PansariLinkButton(onClick = onSignUp, enabled = !state.loading, text = stringResource(Res.string.partner_signup_join))
            }
        }
    }
}

@Composable
fun PartnerLoginOtpScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerLoginViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OtpAutofillEffect(key = state.hint) { code ->
        viewModel.setOtp(code)
        viewModel.verify(onVerified)
    }
    PartnerOtpContent(
        hint = state.hint,
        otp = state.otp,
        loading = state.loading,
        resendInSec = state.resendInSec,
        error = state.error,
        onOtpChange = viewModel::setOtp,
        onVerify = { viewModel.verify(onVerified) },
        onResend = { viewModel.sendOtp() },
        onDismissError = viewModel::dismissError,
        onBack = onBack,
    )
}

@Composable
fun PartnerRegisterDetailsScreen(
    onNext: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerRegisterViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val attachProfile = rememberImageSourceLauncher(RegisterPhoto.PROFILE.feature) {
        viewModel.attachPhoto(RegisterPhoto.PROFILE, it)
    }
    RegisterStepScaffold(
        step = 1,
        stepTitle = stringResource(Res.string.partner_register_step_details),
        error = state.error,
        loading = state.loading,
        onDismissError = viewModel::dismissError,
        onBack = onBack,
    ) {
        val profileUpload = state.photoUploads[RegisterPhoto.PROFILE] ?: PhotoUploadUi()
        PartnerProfileCircle(
            imageUrl = state.profilePhoto,
            onClick = attachProfile,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            showEditBadge = true,
            uploading = profileUpload.uploading,
            localPreviewBytes = profileUpload.previewBytes,
        )
        Text(
            stringResource(Res.string.partner_profile_pic),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(state.name, viewModel::setName, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(state.email, viewModel::setEmail, label = { Text(stringResource(Res.string.field_email)) }, modifier = Modifier.fillMaxWidth())
        PhoneNumberHintEffect(state.phone, viewModel::setPhone)
        OutlinedTextField(state.phone, viewModel::setPhone, label = { Text(stringResource(Res.string.field_phone)) }, modifier = Modifier.fillMaxWidth())
        PartnerPrimaryButton(
            text = stringResource(Res.string.action_continue),
            onClick = { viewModel.continueFromDetails(onNext) },
            enabled = !state.loading,
        )
        PansariLinkButton(onClick = onSignIn, enabled = !state.loading, text = stringResource(Res.string.partner_login_prompt))
    }
}

@Composable
fun PartnerRegisterAddressScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerRegisterViewModel,
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
    RegisterStepScaffold(
        step = 2,
        stepTitle = stringResource(Res.string.partner_register_step_address),
        error = state.error,
        loading = state.loading,
        onDismissError = viewModel::dismissError,
        onBack = onBack,
    ) {
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
        PartnerPrimaryButton(
            text = stringResource(Res.string.action_continue),
            onClick = { viewModel.continueFromAddress(onNext) },
            enabled = !state.loading,
        )
    }
}

@Composable
fun PartnerRegisterVehicleScreen(
    onOtpSent: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerRegisterViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val attachDl = rememberImageSourceLauncher(RegisterPhoto.DL.feature) { viewModel.attachPhoto(RegisterPhoto.DL, it) }
    val attachVehicle = rememberImageSourceLauncher(RegisterPhoto.VEHICLE.feature) {
        viewModel.attachPhoto(RegisterPhoto.VEHICLE, it)
    }
    val attachId = rememberImageSourceLauncher(RegisterPhoto.ID.feature) { viewModel.attachPhoto(RegisterPhoto.ID, it) }
    RegisterStepScaffold(
        step = 3,
        stepTitle = stringResource(Res.string.partner_register_step_vehicle),
        error = state.error,
        loading = state.loading,
        onDismissError = viewModel::dismissError,
        onRetry = { viewModel.save(onOtpSent) },
        onBack = onBack,
    ) {
        OutlinedTextField(state.vehicleReg, viewModel::setVehicleReg, label = { Text(stringResource(Res.string.field_vehicle_reg)) }, modifier = Modifier.fillMaxWidth())
        PartnerDocumentRow(
            stringResource(Res.string.partner_doc_dl),
            state.dlPhoto,
            attachDl,
            upload = state.photoUploads[RegisterPhoto.DL] ?: PhotoUploadUi(),
        )
        Text(
            stringResource(Res.string.hint_vehicle_photo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        PartnerDocumentRow(
            stringResource(Res.string.field_vehicle_photo),
            state.vehiclePhoto,
            attachVehicle,
            upload = state.photoUploads[RegisterPhoto.VEHICLE] ?: PhotoUploadUi(),
        )
        PartnerDocumentRow(
            stringResource(Res.string.partner_doc_id),
            state.idPhoto,
            attachId,
            upload = state.photoUploads[RegisterPhoto.ID] ?: PhotoUploadUi(),
        )
        Text(stringResource(Res.string.partner_verification_status), fontWeight = FontWeight.SemiBold)
        PartnerVerificationBadge(verified = false)
        PartnerPrimaryButton(
            text = stringResource(Res.string.partner_action_sign_up),
            onClick = { viewModel.save(onOtpSent) },
            enabled = !state.loading,
        )
    }
}

@Composable
fun PartnerRegisterOtpScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerRegisterViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OtpAutofillEffect(key = state.hint) { code ->
        viewModel.setOtp(code)
        viewModel.verify(onVerified)
    }
    PartnerOtpContent(
        hint = state.hint,
        otp = state.otp,
        loading = state.loading,
        resendInSec = state.resendInSec,
        error = state.error,
        onOtpChange = viewModel::setOtp,
        onVerify = { viewModel.verify(onVerified) },
        onResend = { viewModel.save() },
        onDismissError = viewModel::dismissError,
        onBack = onBack,
    )
}

@Composable
private fun RegisterStepScaffold(
    step: Int,
    stepTitle: String,
    error: UiText?,
    loading: Boolean,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    PansariScreen(
        topBar = { PartnerTopBar(title = stringResource(Res.string.partner_register_title), onBack = onBack) },
        error = error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = onRetry, onDismiss = onDismissError) },
        isLoading = loading,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.onboarding_step_of, step, REGISTER_FORM_STEPS, stepTitle),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun PartnerOtpContent(
    hint: UiText?,
    otp: String,
    loading: Boolean,
    resendInSec: Int,
    error: UiText?,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
) {
    PansariScreen(
        topBar = { PartnerTopBar(title = stringResource(Res.string.onboarding_otp_heading), onBack = onBack) },
        error = error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = onResend, onDismiss = onDismissError) },
        isLoading = loading,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            hint?.let {
                Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OtpCodeField(value = otp, onValueChange = onOtpChange, enabled = !loading, onDone = onVerify)
            PartnerPrimaryButton(
                text = stringResource(Res.string.action_verify_otp),
                onClick = onVerify,
                enabled = !loading && otp.length == AppConstants.OTP_LENGTH,
            )
            ResendOtpButton(resendInSec = resendInSec, loading = loading, onClick = onResend)
        }
    }
}
