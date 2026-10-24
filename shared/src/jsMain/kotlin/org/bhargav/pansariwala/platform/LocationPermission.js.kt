package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny

class JsDeviceLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint = requestBrowserLocation()
}

/** `GeolocationPositionError` codes. */
private const val GEO_PERMISSION_DENIED = 1
private const val GEO_TIMEOUT = 3

/** Desktops often have no GPS: a high-accuracy request can time out where a Wi-Fi fix succeeds. */
internal suspend fun requestBrowserLocation(): GeoPoint = try {
    browserFix(highAccuracy = true)
} catch (_: LocationUnavailableException) {
    browserFix(highAccuracy = false)
}

private suspend fun browserFix(highAccuracy: Boolean): GeoPoint = suspendCancellableCoroutine { cont ->
    val geolocation = window.asDynamic().navigator.geolocation
    if (geolocation == undefined) {
        cont.resumeWithException(LocationUnavailableException())
        return@suspendCancellableCoroutine
    }
    val options: dynamic = js("({})")
    options.enableHighAccuracy = highAccuracy
    options.timeout = if (highAccuracy) AppConstants.LOCATION_FETCH_TIMEOUT_MS.toInt() else AppConstants.LOCATION_COARSE_TIMEOUT_MS.toInt()
    options.maximumAge = AppConstants.LOCATION_MAX_AGE_MS.toInt()
    geolocation.getCurrentPosition(
        { position: JsAny? ->
            val coords = position.asDynamic().coords
            cont.resume(
                GeoPoint(
                    lat = coords.latitude as Double,
                    lng = coords.longitude as Double,
                ),
            )
        },
        { error: JsAny? ->
            val code = error.asDynamic().code as? Int ?: 1
            cont.resumeWithException(
                when (code) {
                    GEO_PERMISSION_DENIED -> LocationPermissionDeniedException()
                    GEO_TIMEOUT -> LocationTimeoutException()
                    else -> LocationPositionUnavailableException()
                },
            )
        },
        options,
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
