package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.domain.model.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny

class WasmJsDeviceLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint = requestBrowserLocation()
}

internal suspend fun requestBrowserLocation(): GeoPoint = suspendCancellableCoroutine { cont ->
    val geolocation = window.asDynamic().navigator.geolocation
    if (geolocation == undefined) {
        cont.resumeWithException(LocationUnavailableException())
        return@suspendCancellableCoroutine
    }
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
                if (code == 1) LocationPermissionDeniedException() else LocationUnavailableException(),
            )
        },
        js("({ enableHighAccuracy: true, timeout: 20000, maximumAge: 15000 })"),
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

actual fun openAppLocationSettings() = Unit
