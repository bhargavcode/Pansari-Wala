package org.bhargav.pansariwala.platform

import kotlinx.coroutines.delay
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.util.AppConstants

/**
 * Keeps the process able to push partner GPS while online (FGS on Android, background
 * location on iOS). The actual 2-minute loop lives in the platform keep-alive.
 */
expect fun startPartnerLocationTracking()

expect fun stopPartnerLocationTracking()

internal suspend fun pushPartnerLocationOnce(location: DeviceLocation, api: PansariApi) {
    val geo = location.currentOrDefault()
    api.updatePartnerLocation(geo.lat, geo.lng)
}

internal suspend fun partnerLocationUpdateLoop(location: DeviceLocation, api: PansariApi) {
    while (true) {
        runCatching { pushPartnerLocationOnce(location, api) }
        delay(AppConstants.PARTNER_LOCATION_UPDATE_MS)
    }
}

class PartnerLocationTracker(
    private val preferences: AppPreferences,
) {
    suspend fun setOnlineDuty(online: Boolean) {
        preferences.setPartnerOnlineDuty(online)
        if (online && preferences.hasSession()) {
            startPartnerLocationTracking()
        } else {
            stopPartnerLocationTracking()
        }
    }

    suspend fun restore() {
        if (preferences.getPartnerOnlineDuty() && preferences.hasSession()) {
            startPartnerLocationTracking()
        } else {
            stopPartnerLocationTracking()
        }
    }
}
