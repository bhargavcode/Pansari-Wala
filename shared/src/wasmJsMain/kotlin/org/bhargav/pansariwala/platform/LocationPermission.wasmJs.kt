package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import js.objects.unsafeJso
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants
import web.geolocation.GeolocationPositionError
import web.geolocation.getCurrentPosition
import web.navigator.navigator

class WasmJsDeviceLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint = requestBrowserLocation()
}

/** Desktops often have no GPS: a high-accuracy request can time out where a Wi-Fi fix succeeds. */
internal suspend fun requestBrowserLocation(): GeoPoint = try {
    browserFix(highAccuracy = true)
} catch (_: LocationUnavailableException) {
    browserFix(highAccuracy = false)
}

private suspend fun browserFix(highAccuracy: Boolean): GeoPoint {
    val position = try {
        navigator.geolocation.getCurrentPosition(
            unsafeJso {
                enableHighAccuracy = highAccuracy
                timeout = if (highAccuracy) AppConstants.LOCATION_FETCH_TIMEOUT_MS.toInt() else AppConstants.LOCATION_COARSE_TIMEOUT_MS.toInt()
                maximumAge = AppConstants.LOCATION_MAX_AGE_MS.toInt()
            },
        )
    } catch (e: Throwable) {
        val denied = e.message?.contains("denied", ignoreCase = true) == true ||
            e.message?.contains("${GeolocationPositionError.PERMISSION_DENIED}", ignoreCase = false) == true
        throw if (denied) LocationPermissionDeniedException() else LocationUnavailableException()
    }
    return GeoPoint(
        lat = position.coords.latitude,
        lng = position.coords.longitude,
    )
}

@Composable
actual fun RequestLocationPermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        val granted = runCatching { requestBrowserLocation() }.isSuccess
        onConsumed()
        onResult(granted)
    }
}

actual fun canOpenLocationSettings(): Boolean = false

actual fun isLocationPermissionGranted(): Boolean = true

actual fun openAppLocationSettings() = Unit
