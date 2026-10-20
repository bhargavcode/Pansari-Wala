package org.bhargav.pansariwala.platform

import android.content.Context
import android.os.Build
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun appBuildInfo(): AppBuildInfo {
    val holder = object : KoinComponent {
        val context: Context by inject()
    }
    val context = holder.context.applicationContext
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val code = when {
        info == null -> 0L
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
        else -> @Suppress("DEPRECATION") info.versionCode.toLong()
    }
    return AppBuildInfo(
        versionName = info?.versionName.orEmpty(),
        versionCode = code,
        platform = AppConstants.Push.PLATFORM_ANDROID,
    )
}
