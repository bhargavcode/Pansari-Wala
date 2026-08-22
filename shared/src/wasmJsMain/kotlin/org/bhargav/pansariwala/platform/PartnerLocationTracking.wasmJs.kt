package org.bhargav.pansariwala.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.koin.core.context.GlobalContext

private val trackingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var trackingJob: Job? = null

actual fun startPartnerLocationTracking() {
    if (trackingJob?.isActive == true) return
    val koin = runCatching { GlobalContext.get() }.getOrNull() ?: return
    trackingJob = trackingScope.launch {
        partnerLocationUpdateLoop(koin.get<DeviceLocation>(), koin.get<PansariApi>())
    }
}

actual fun stopPartnerLocationTracking() {
    trackingJob?.cancel()
    trackingJob = null
}
