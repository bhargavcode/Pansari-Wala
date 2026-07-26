package org.bhargav.pansariwala.voice

import kotlinx.coroutines.flow.SharedFlow

/**
 * Platform speech-to-text bridge. Configured for Hindi (`hi-IN`) where the OS supports it.
 */
interface SpeechToText {
    val events: SharedFlow<SpeechEvent>

    fun isAvailable(): Boolean
    fun startListening()
    fun stopListening()
    fun cancel()
}

sealed interface SpeechEvent {
    data object Started : SpeechEvent
    data class PartialResult(val text: String) : SpeechEvent
    data class FinalResult(val text: String) : SpeechEvent
    data class Error(val message: String, val needsPermission: Boolean = false) : SpeechEvent
    data object Ended : SpeechEvent
}

expect fun createSpeechToText(): SpeechToText
