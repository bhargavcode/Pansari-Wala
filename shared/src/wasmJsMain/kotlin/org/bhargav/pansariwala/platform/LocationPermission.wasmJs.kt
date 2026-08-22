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
        cont.resumeWithException(LocationPermissionDeniedException())
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
        { _: JsAny? ->
            cont.resumeWithException(LocationPermissionDeniedException())
        },
        js("({ enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })"),
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
        onConsumed()
        runCatching { requestBrowserLocation() }
            .onSuccess { onResult(true) }
            .onFailure { onResult(false) }
    }
}

actual fun canOpenLocationSettings(): Boolean = false

actual fun openAppLocationSettings() = Unit
