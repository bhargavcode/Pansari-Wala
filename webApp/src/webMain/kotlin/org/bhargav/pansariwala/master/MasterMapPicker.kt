package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.platform.pickLocationOnMap
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_latitude
import pansariwala.shared.generated.resources.master_longitude
import pansariwala.shared.generated.resources.master_map_location
import pansariwala.shared.generated.resources.map_picker_unavailable
import pansariwala.shared.generated.resources.master_select_on_map

private const val PICKER_ZOOM = 16
private const val COORD_DECIMALS = 1_000_000.0
private const val DEFAULT_COUNTRY = "India"

/** Address parts reverse-geocoded from the picked point; blank when unknown. */
data class PickedAddress(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String,
)

/**
 * Shop location: editable coordinates plus a Google Maps picker (Places search, centre pin).
 * [onAddressResolved] gets the picked point's address so the form can fill empty fields.
 */
@Composable
fun MapLocationPicker(
    lat: Double,
    lng: Double,
    onLatChange: (Double) -> Unit,
    onLngChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    onAddressResolved: (PickedAddress) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var latText by remember { mutableStateOf(lat.toString()) }
    var lngText by remember { mutableStateOf(lng.toString()) }
    var picking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(lat, lng) {
        latText = lat.toString()
        lngText = lng.toString()
    }

    fun round(v: Double) = (v * COORD_DECIMALS).roundToInt() / COORD_DECIMALS

    Column(modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_map_location), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { v -> latText = v; v.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }?.let(onLatChange) },
                label = { Text(stringResource(Res.string.master_latitude)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { v -> lngText = v; v.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }?.let(onLngChange) },
                label = { Text(stringResource(Res.string.master_longitude)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedButton(
            onClick = {
                if (picking) return@OutlinedButton
                picking = true
                failed = false
                scope.launch {
                    val picked = runCatching { pickLocationOnMap(lat, lng, PICKER_ZOOM) }
                        .onFailure { failed = true }
                        .getOrNull()
                    picking = false
                    picked ?: return@launch
                    onLatChange(round(picked.lat))
                    onLngChange(round(picked.lng))
                    onAddressResolved(PickedAddress(picked.address, picked.city, picked.state, picked.zip, DEFAULT_COUNTRY))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (picking) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
                Text(stringResource(Res.string.master_select_on_map), Modifier.padding(start = 8.dp))
            }
        }
        if (failed) {
            Text(
                stringResource(Res.string.map_picker_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}
