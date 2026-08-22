package org.bhargav.pansariwala.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.currentAppProduct
import org.koin.mp.KoinPlatform

private val trackingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var trackingJob: Job? = null

actual fun startPartnerLocationTracking() {
    if (currentAppProduct() != AppProduct.DELIVERY) return
    IosLocationEngine.startBackgroundTracking()
    if (trackingJob?.isActive == true) return
    val koin = runCatching { KoinPlatform.getKoin() }.getOrNull() ?: return
    val location = koin.get<DeviceLocation>()
    val api = koin.get<PansariApi>()
    trackingJob = trackingScope.launch {
        partnerLocationUpdateLoop(location, api)
    }
}

actual fun stopPartnerLocationTracking() {
    trackingJob?.cancel()
    trackingJob = null
    IosLocationEngine.stopBackgroundTracking()
}
