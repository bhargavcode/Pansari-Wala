package org.bhargav.pansariwala.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.context.GlobalContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only describes the request to Play Services' "turn on location" check; no updates are started. */
private const val LOCATION_SETTINGS_INTERVAL_MS = 10_000L

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

internal fun Context.hasLocationPermission(): Boolean =
    locationPermissions.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

private fun Context.isLocationEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

class AndroidDeviceLocation(
    private val context: Context,
) : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint {
        if (!context.hasLocationPermission()) {
            throw LocationPermissionDeniedException()
        }
        if (!context.isLocationEnabled()) {
            throw LocationServicesDisabledException()
        }
        lastKnown(AppConstants.LOCATION_MAX_AGE_MS)?.let { return it }
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val fusedLast = awaitLastLocation(fused)
        if (fusedLast != null && fusedLast.isUsable() && fusedLast.isFresh()) {
            return GeoPoint(fusedLast.latitude, fusedLast.longitude)
        }
        return withTimeout(AppConstants.LOCATION_FETCH_TIMEOUT_MS) {
            val current = awaitCurrentLocation(fused, Priority.PRIORITY_HIGH_ACCURACY)
                ?: awaitCurrentLocation(fused, Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            if (current != null && current.isUsable()) {
                return@withTimeout GeoPoint(current.latitude, current.longitude)
            }
            val oneShot = runCatching { awaitSingleUpdate(fused) }.getOrNull()
            if (oneShot != null && oneShot.isUsable()) {
                return@withTimeout GeoPoint(oneShot.latitude, oneShot.longitude)
            }
            lastKnown() ?: throw LocationUnavailableException()
        }
    }

    private fun lastKnown(maxAgeMs: Long? = null): GeoPoint? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cached = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { it.isUsable() && (maxAgeMs == null || it.isFresh(maxAgeMs)) }
            .maxByOrNull { it.time }
        return cached?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun Location.isUsable(): Boolean = !(latitude == 0.0 && longitude == 0.0)

    private fun Location.isFresh(maxAgeMs: Long = AppConstants.LOCATION_MAX_AGE_MS): Boolean =
        System.currentTimeMillis() - time <= maxAgeMs

    private suspend fun awaitCurrentLocation(
        fused: com.google.android.gms.location.FusedLocationProviderClient,
        priority: Int,
    ): Location? {
        val tokenSource = CancellationTokenSource()
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { tokenSource.cancel() }
            fused.getCurrentLocation(priority, tokenSource.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private suspend fun awaitLastLocation(
        fused: com.google.android.gms.location.FusedLocationProviderClient,
    ): Location? = suspendCancellableCoroutine { cont ->
        fused.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    private suspend fun awaitSingleUpdate(
        fused: com.google.android.gms.location.FusedLocationProviderClient,
    ): Location = suspendCancellableCoroutine { cont ->
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                fused.removeLocationUpdates(this)
                if (loc != null) {
                    if (cont.isActive) cont.resume(loc)
                } else if (cont.isActive) {
                    cont.resumeWithException(LocationUnavailableException())
                }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .build()
        runCatching {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }.onFailure {
            if (cont.isActive) cont.resumeWithException(LocationUnavailableException())
        }
        cont.invokeOnCancellation { fused.removeLocationUpdates(callback) }
    }
}

@Composable
actual fun RequestLocationPermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    var showServicesOff by remember { mutableStateOf(false) }
    var awaitingSettingsReturn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        latestOnResult(results.values.any { it })
    }

    fun requestPermission() {
        if (context.hasLocationPermission()) latestOnResult(true) else permissionLauncher.launch(locationPermissions)
    }

    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || context.isLocationEnabled()) {
            requestPermission()
        } else {
            latestOnResult(false)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!awaitingSettingsReturn) return@LifecycleEventEffect
        awaitingSettingsReturn = false
        if (context.isLocationEnabled()) requestPermission() else latestOnResult(false)
    }

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        onConsumed()
        if (context.isLocationEnabled()) {
            requestPermission()
            return@LaunchedEffect
        }
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_SETTINGS_INTERVAL_MS).build(),
            )
            .setAlwaysShow(true)
            .build()
        LocationServices.getSettingsClient(context).checkLocationSettings(settingsRequest)
            .addOnSuccessListener { requestPermission() }
            .addOnFailureListener { error ->
                val resolvable = error as? ResolvableApiException
                val launched = resolvable != null && runCatching {
                    resolutionLauncher.launch(IntentSenderRequest.Builder(resolvable.resolution).build())
                }.isSuccess
                if (!launched) showServicesOff = true
            }
    }

    LocationServicesOffDialog(
        visible = showServicesOff,
        onTurnOn = {
            showServicesOff = false
            awaitingSettingsReturn = true
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onDismiss = {
            showServicesOff = false
            latestOnResult(false)
        },
    )
}

actual fun canOpenLocationSettings(): Boolean = true

actual fun isLocationPermissionGranted(): Boolean = GlobalContext.get().get<Context>().hasLocationPermission()

actual fun openAppLocationSettings() {
    val context = GlobalContext.get().get<Context>()
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
