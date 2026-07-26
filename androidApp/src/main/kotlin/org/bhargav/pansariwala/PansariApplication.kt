package org.bhargav.pansariwala

import android.app.Application
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.notification.NotificationGateway
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.logger.Level

class PansariApplication : Application(), KoinComponent {
    private val notificationGateway: NotificationGateway by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PansariApplication)
        }
        notificationGateway.ensureChannels()
    }
}
