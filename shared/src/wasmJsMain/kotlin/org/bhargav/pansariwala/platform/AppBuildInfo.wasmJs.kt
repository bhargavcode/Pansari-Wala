package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.util.AppConstants

actual fun appBuildInfo(): AppBuildInfo =
    AppBuildInfo(versionName = AppConstants.Support.WEB_VERSION, versionCode = 0L, platform = AppConstants.Support.PLATFORM_WEB)
