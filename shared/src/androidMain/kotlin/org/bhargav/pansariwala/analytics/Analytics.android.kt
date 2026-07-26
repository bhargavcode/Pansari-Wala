package org.bhargav.pansariwala.analytics

import android.util.Log

private class AndroidAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) {
        // Ready to swap for FirebaseAnalytics.logEvent(event.name, Bundle)
        Log.i(
            "Analytics",
            "event=${event.name} params=${event.params}",
        )
    }
}

actual fun createAnalytics(): Analytics = AndroidAnalytics()
