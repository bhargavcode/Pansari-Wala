package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants

interface DeviceLocation {
    suspend fun currentOrDefault(): GeoPoint
}

class LocationPermissionDeniedException : Exception("Location permission denied")

open class LocationUnavailableException(message: String = "Location unavailable") : Exception(message)

/** Device location (GPS) switch is off; re-run [RequestLocationPermission] to prompt the user to turn it on. */
class LocationServicesDisabledException : LocationUnavailableException("Location services disabled")

/** Browser had permission but the OS gave no position (e.g. macOS Location Services off for the browser). */
class LocationPositionUnavailableException : LocationUnavailableException("Position unavailable")

/** No fix arrived before the timeout. */
class LocationTimeoutException : LocationUnavailableException("Location timeout")

class FallbackLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint =
        GeoPoint(AppConstants.DEFAULT_MAP_LAT, AppConstants.DEFAULT_MAP_LNG)
}
