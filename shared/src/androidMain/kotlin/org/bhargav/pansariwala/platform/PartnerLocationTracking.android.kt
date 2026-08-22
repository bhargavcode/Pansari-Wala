package org.bhargav.pansariwala.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProductHolder
import org.bhargav.pansariwala.shared.R
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

actual fun startPartnerLocationTracking() {
    if (AppProductHolder.current != AppProduct.DELIVERY) return
    val context = GlobalContext.get().get<Context>()
    val intent = Intent(context, PartnerLocationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

actual fun stopPartnerLocationTracking() {
    val context = runCatching { GlobalContext.get().get<Context>() }.getOrNull() ?: return
    context.stopService(Intent(context, PartnerLocationService::class.java))
}

class PartnerLocationService : Service(), KoinComponent {
    private val location: DeviceLocation by inject()
    private val api: PansariApi by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        if (loopJob?.isActive != true) {
            loopJob = scope.launch {
                partnerLocationUpdateLoop(location, api)
            }
        }
        return START_STICKY
    }

    private fun promoteToForeground() {
        ensureChannel()
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, AppConstants.Notification.CHANNEL_LOCATION)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle(getString(R.string.partner_location_service_title))
            .setContentText(getString(R.string.partner_location_service_body))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AppConstants.Notification.LOCATION_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(AppConstants.Notification.LOCATION_SERVICE_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            AppConstants.Notification.CHANNEL_LOCATION,
            getString(R.string.partner_location_service_title),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.partner_location_service_body)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        loopJob?.cancel()
        loopJob = null
        scope.cancel()
        super.onDestroy()
    }
}
