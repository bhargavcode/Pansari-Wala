package org.bhargav.pansariwala

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProductHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.logger.Level

class PansariApplication : Application(), KoinComponent {
    private val notificationGateway: NotificationGateway by inject()
    private val locationTracker: PartnerLocationTracker by inject()

    override fun onCreate() {
        super.onCreate()
        AppProductHolder.current = AppProduct.fromName(BuildConfig.APP_PRODUCT)
        ApiRuntime.baseUrl = BuildConfig.API_BASE_URL
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PansariApplication)
        }
        notificationGateway.ensureChannels()
        if (AppProductHolder.current == AppProduct.DELIVERY) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                locationTracker.restore()
            }
        }
    }
}
