package org.bhargav.pansariwala.feature.delivery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.GeoDistance

/**
 * Client-only proximity check (no API). Polls on a background dispatcher so UI stays free.
 */
@Composable
fun rememberWithinArrivalRadius(
    destLat: Double,
    destLng: Double,
    deviceLocation: DeviceLocation,
    radiusMeters: Double = AppConstants.ARRIVAL_PROXIMITY_RADIUS_M,
    pollMs: Long = AppConstants.ARRIVAL_PROXIMITY_POLL_MS,
): Boolean {
    var within by remember(destLat, destLng) { mutableStateOf(false) }
    LaunchedEffect(destLat, destLng, radiusMeters, pollMs) {
        while (true) {
            val nearby = withContext(Dispatchers.Default) {
                val here = runCatching { deviceLocation.currentOrDefault() }.getOrNull()
                    ?: return@withContext false
                GeoDistance.isWithinMeters(
                    here.lat,
                    here.lng,
                    destLat,
                    destLng,
                    radiusMeters,
                )
            }
            within = nearby
            delay(pollMs)
        }
    }
    return within
}
