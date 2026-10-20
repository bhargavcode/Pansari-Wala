package org.bhargav.pansariwala.notification

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bhargav.pansariwala.api.AppInstallRequest
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.platform.appBuildInfo
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.util.AppConstants

/** Reports this build to the server once per app start and per signed-in session. */
class AppInstallReporter(
    private val api: PansariApi,
    private val preferences: AppPreferences,
) {
    suspend fun run(product: AppProduct) {
        val build = appBuildInfo()
        preferences.accessToken
            .map { jwt -> jwt?.takeIf { it.startsWith(AppConstants.JWT_PREFIX) } }
            .distinctUntilChanged()
            .collectLatest { jwt ->
                if (jwt == null) return@collectLatest
                val request = AppInstallRequest(
                    app = product.name,
                    platform = build.platform,
                    versionName = build.versionName,
                    versionCode = build.versionCode,
                    deviceId = preferences.getOrCreateDeviceId(),
                )
                while (runCatching { api.reportAppInstall(request) }.isFailure) {
                    delay(AppConstants.Support.INSTALL_RETRY_MS)
                }
            }
    }
}
