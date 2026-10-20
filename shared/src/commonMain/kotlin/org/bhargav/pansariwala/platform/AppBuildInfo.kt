package org.bhargav.pansariwala.platform

/** Installed build of this app; [platform] matches the push platform names (`android`, `ios`, `web`). */
data class AppBuildInfo(
    val versionName: String,
    val versionCode: Long,
    val platform: String,
)

expect fun appBuildInfo(): AppBuildInfo
