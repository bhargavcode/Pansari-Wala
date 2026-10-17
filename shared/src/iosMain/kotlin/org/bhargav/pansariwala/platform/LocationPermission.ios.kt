package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
object IosLocationEngine {
    private var lastFix: GeoPoint? = null
    private var pendingFix: ((Result<GeoPoint>) -> Unit)? = null
    private var pendingAuth: ((Boolean) -> Unit)? = null
    private var backgroundActive: Boolean = false

    private val locationDelegate: CLLocationManagerDelegateProtocol =
        object : NSObject(), CLLocationManagerDelegateProtocol {
            @ObjCSignatureOverride
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                val point = loc.coordinate.useContents { GeoPoint(latitude, longitude) }
                lastFix = point
                pendingFix?.invoke(Result.success(point))
                pendingFix = null
            }

            @ObjCSignatureOverride
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                pendingFix?.invoke(Result.failure(LocationUnavailableException()))
                pendingFix = null
            }

            @ObjCSignatureOverride
            override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                handleAuth(didChangeAuthorizationStatus)
            }

            @ObjCSignatureOverride
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                handleAuth(manager.authorizationStatus)
            }
        }

    private val manager: CLLocationManager = CLLocationManager().also { mgr ->
        mgr.delegate = locationDelegate
        mgr.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    private fun handleAuth(status: CLAuthorizationStatus) {
        if (status == kCLAuthorizationStatusNotDetermined) return
        val granted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
        pendingAuth?.invoke(granted)
        pendingAuth = null
        if (backgroundActive && granted) {
            applyBackgroundSettings()
            manager.startUpdatingLocation()
        }
    }

    fun isGranted(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    fun requestWhenInUse(onResult: (Boolean) -> Unit) {
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> onResult(true)
            kCLAuthorizationStatusNotDetermined -> {
                pendingAuth = onResult
                manager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> onResult(false)
            else -> onResult(false)
        }
    }

    fun startBackgroundTracking() {
        backgroundActive = true
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedAlways -> {
                applyBackgroundSettings()
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse -> {
                manager.requestAlwaysAuthorization()
                applyBackgroundSettings()
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusNotDetermined -> {
                pendingAuth = { granted ->
                    if (granted) {
                        manager.requestAlwaysAuthorization()
                        applyBackgroundSettings()
                        manager.startUpdatingLocation()
                    }
                }
                manager.requestWhenInUseAuthorization()
            }
            else -> Unit
        }
    }

    fun stopBackgroundTracking() {
        backgroundActive = false
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
    }

    private fun applyBackgroundSettings() {
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.showsBackgroundLocationIndicator = true
    }

    suspend fun currentFix(): GeoPoint = withContext(Dispatchers.Main) {
        if (!isGranted()) throw LocationPermissionDeniedException()
        if (!locationServicesEnabled()) throw LocationServicesDisabledException()
        val cached = lastFix
        try {
            withTimeout(AppConstants.LOCATION_FETCH_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    pendingFix = { result ->
                        result
                            .onSuccess { if (cont.isActive) cont.resume(it) }
                            .onFailure { if (cont.isActive) cont.resumeWithException(it) }
                    }
                    manager.requestLocation()
                    manager.startUpdatingLocation()
                    cont.invokeOnCancellation { pendingFix = null }
                }
            }
        } catch (_: TimeoutCancellationException) {
            cached ?: throw LocationUnavailableException()
        } finally {
            if (!backgroundActive) manager.stopUpdatingLocation()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosDeviceLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint = IosLocationEngine.currentFix()
}

/** Checked off the main thread: Apple warns the call can block UI. */
private suspend fun locationServicesEnabled(): Boolean =
    withContext(Dispatchers.Default) { CLLocationManager.locationServicesEnabled() }

@Composable
actual fun RequestLocationPermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    val latestOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    var showServicesOff by remember { mutableStateOf(false) }
    var awaitingSettingsReturn by remember { mutableStateOf(false) }

    fun requestPermission() {
        IosLocationEngine.requestWhenInUse { granted -> latestOnResult(granted) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!awaitingSettingsReturn) return@LifecycleEventEffect
        awaitingSettingsReturn = false
        scope.launch {
            if (locationServicesEnabled()) requestPermission() else latestOnResult(false)
        }
    }

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        onConsumed()
        if (locationServicesEnabled()) requestPermission() else showServicesOff = true
    }

    LocationServicesOffDialog(
        visible = showServicesOff,
        onTurnOn = {
            showServicesOff = false
            awaitingSettingsReturn = true
            openAppLocationSettings()
        },
        onDismiss = {
            showServicesOff = false
            latestOnResult(false)
        },
    )
}

actual fun canOpenLocationSettings(): Boolean = true

actual fun openAppLocationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
}
