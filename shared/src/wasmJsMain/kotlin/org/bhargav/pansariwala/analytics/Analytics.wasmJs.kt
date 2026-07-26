package org.bhargav.pansariwala.analytics

private class WasmAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) {
        println("Analytics event=${event.name} params=${event.params}")
    }
}

actual fun createAnalytics(): Analytics = WasmAnalytics()
