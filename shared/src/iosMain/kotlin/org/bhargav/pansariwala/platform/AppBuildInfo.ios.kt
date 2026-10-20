package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.util.AppConstants
import platform.Foundation.NSBundle

actual fun appBuildInfo(): AppBuildInfo {
    val bundle = NSBundle.mainBundle
    val name = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
    return AppBuildInfo(
        versionName = name.orEmpty(),
        versionCode = build?.toLongOrNull() ?: 0L,
        platform = AppConstants.Push.PLATFORM_IOS,
    )
}
