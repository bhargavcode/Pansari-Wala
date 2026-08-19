package org.bhargav.pansariwala.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.util.AppConstants

class AndroidDeviceLocation(
    private val context: Context,
) : DeviceLocation {
    @SuppressLint("MissingPermission")
    override suspend fun currentOrDefault(): GeoPoint {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val last = manager.getProviders(true).firstNotNullOfOrNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        return if (last != null) {
            GeoPoint(last.latitude, last.longitude)
        } else {
            GeoPoint(AppConstants.DEFAULT_MAP_LAT, AppConstants.DEFAULT_MAP_LNG)
        }
    }
}
