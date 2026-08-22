package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.i18n.asString
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
import pansariwala.shared.generated.resources.location_confirm_address
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
    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            PansariTopBar(title = title, onBack = onBack)
        } else {
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
        TextButton(onClick = viewModel::useCurrentLocation, enabled = !state.loading) {
            Text(stringResource(Res.string.action_use_current_location))
        }
        UserPrimaryButton(
            text = confirmLabel,
            onClick = { viewModel.save(requireName, onDone) },
            enabled = !state.loading &&
                state.address.isNotBlank() &&
                state.locality.isNotBlank() &&
                (!requireName || state.name.isNotBlank()),
        )
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        if (state.loading) CircularProgressIndicator()
    }
}
