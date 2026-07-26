package org.bhargav.pansariwala.analytics

/**
 * Typed analytics events with key/value payloads.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, String>

    data class ScreenView(
        val fromScreen: String?,
        val toScreen: String,
    ) : AnalyticsEvent {
        override val name: String = "screen_view"
        override val params: Map<String, String> = buildMap {
            put("to_screen", toScreen)
            fromScreen?.let { put("from_screen", it) }
        }
    }

    data class ButtonClicked(
        val buttonId: String,
        val screen: String,
        val extra: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "button_clicked"
        override val params: Map<String, String> = buildMap {
            put("button_id", buttonId)
            put("screen", screen)
            putAll(extra)
        }
    }

    data class PopOpened(
        val popId: String,
        val screen: String,
        val extra: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "pop_opened"
        override val params: Map<String, String> = buildMap {
            put("pop_id", popId)
            put("screen", screen)
            putAll(extra)
        }
    }

    data class Error(
        val code: String,
        val message: String,
        val payload: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "app_error"
        override val params: Map<String, String> = buildMap {
            put("error_code", code)
            put("error_message", message)
            putAll(payload)
        }
    }
}

interface Analytics {
    fun log(event: AnalyticsEvent)
}

expect fun createAnalytics(): Analytics
