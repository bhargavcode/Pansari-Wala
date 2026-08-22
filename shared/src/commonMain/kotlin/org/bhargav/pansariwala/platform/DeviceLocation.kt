package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants

interface DeviceLocation {
    suspend fun currentOrDefault(): GeoPoint
}

class LocationPermissionDeniedException : Exception("Location permission denied")

class LocationUnavailableException : Exception("Location unavailable")

class FallbackLocation : DeviceLocation {
    override suspend fun currentOrDefault(): GeoPoint =
        GeoPoint(AppConstants.DEFAULT_MAP_LAT, AppConstants.DEFAULT_MAP_LNG)
}
