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
import org.bhargav.pansariwala.domain.model.GeoPoint
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

/**
 * Partner position for route maps. Re-polled while the screen is open and only replaced after a real
 * move, so the drawn route follows the rider without re-requesting directions on GPS jitter.
 */
@Composable
fun rememberPartnerOrigin(
    key: Any?,
    deviceLocation: DeviceLocation,
): GeoPoint? {
    var origin by remember(key) { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(key) {
        while (true) {
            val here = withContext(Dispatchers.Default) {
                runCatching { deviceLocation.currentOrDefault() }.getOrNull()
            }
            val last = origin
            if (here != null && (last == null ||
                    !GeoDistance.isWithinMeters(last.lat, last.lng, here.lat, here.lng, AppConstants.PARTNER_ROUTE_REFRESH_MOVE_M))
            ) {
                origin = here
            }
            delay(AppConstants.PARTNER_ROUTE_REFRESH_MS)
        }
    }
    return origin
}
