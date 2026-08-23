package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
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

@Composable
actual fun RequestLocationPermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        IosLocationEngine.requestWhenInUse { granted ->
            onConsumed()
            onResult(granted)
        }
    }
}

actual fun canOpenLocationSettings(): Boolean = true

actual fun openAppLocationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
}
