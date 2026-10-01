package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PermissionRationaleSheet
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.bhargav.pansariwala.ui.toErrorBanner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_continue
import pansariwala.shared.generated.resources.action_use_current_location
import pansariwala.shared.generated.resources.address_screen_title
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_locality
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_place_search
import pansariwala.shared.generated.resources.hint_address_pick_place
import pansariwala.shared.generated.resources.location_address_filled
import pansariwala.shared.generated.resources.location_confirm_address
import pansariwala.shared.generated.resources.location_detecting_address
import pansariwala.shared.generated.resources.location_rationale_allow
import pansariwala.shared.generated.resources.location_rationale_footer
import pansariwala.shared.generated.resources.location_rationale_message
import pansariwala.shared.generated.resources.location_rationale_point_address
import pansariwala.shared.generated.resources.location_rationale_point_delivery
import pansariwala.shared.generated.resources.location_rationale_point_shops
import pansariwala.shared.generated.resources.location_rationale_title
import pansariwala.shared.generated.resources.place_search_no_results
import pansariwala.shared.generated.resources.place_search_searching
import pansariwala.shared.generated.resources.user_location_permission_denied_message
import pansariwala.shared.generated.resources.profile_setup_title

@Composable
fun ProfileSetupScreen(
    onDone: () -> Unit,
    viewModel: AddressViewModel = koinViewModel(),
) {
    AddressForm(
        title = stringResource(Res.string.profile_setup_title),
        requireName = true,
        confirmLabel = stringResource(Res.string.location_confirm_address),
        onDone = onDone,
        onBack = null,
        viewModel = viewModel,
    )
}

@Composable
fun AddressScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddressViewModel = koinViewModel(),
) {
    AddressForm(
        title = stringResource(Res.string.address_screen_title),
        requireName = false,
        confirmLabel = stringResource(Res.string.action_continue),
        onDone = onDone,
        onBack = onBack,
        viewModel = viewModel,
    )
}

@Composable
private fun AddressForm(
    title: String,
    requireName: Boolean,
    confirmLabel: String,
    onDone: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: AddressViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.showLocationRationale) {
        PermissionRationaleSheet(
            icon = Icons.Default.LocationOn,
            title = stringResource(Res.string.location_rationale_title),
            message = stringResource(Res.string.location_rationale_message),
            points = listOf(
                stringResource(Res.string.location_rationale_point_address),
                stringResource(Res.string.location_rationale_point_shops),
                stringResource(Res.string.location_rationale_point_delivery),
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
        message = stringResource(Res.string.user_location_permission_denied_message),
    )
    PansariScreen(
        title = if (onBack != null) title else null,
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading,
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack == null) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        if (requireName) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(Res.string.field_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = state.placeQuery,
            onValueChange = viewModel::setPlaceQuery,
            label = { Text(stringResource(Res.string.field_place_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (state.searchingPlaces) {
                { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
            } else {
                null
            },
            supportingText = when {
                state.searchingPlaces -> { { Text(stringResource(Res.string.place_search_searching)) } }
                state.noPlaceResults -> { { Text(stringResource(Res.string.place_search_no_results)) } }
                else -> null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.predictions.isNotEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                state.predictions.forEachIndexed { index, prediction ->
                    if (index > 0) HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(prediction.description, style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.clickable { viewModel.selectPlace(prediction.placeId) },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = viewModel::useCurrentLocation,
            enabled = !state.loading && !state.locating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.locating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    if (state.locating) Res.string.location_detecting_address else Res.string.action_use_current_location,
                ),
            )
        }
        if (state.locationFilled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(Res.string.location_address_filled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
        UserPrimaryButton(
            text = confirmLabel,
            onClick = { viewModel.save(requireName, onDone) },
            enabled = !state.loading && !state.locating &&
                state.address.isNotBlank() &&
                state.locality.isNotBlank() &&
                (!requireName || state.name.isNotBlank()),
        )
        }
    }
}
