package org.bhargav.pansariwala.voice

import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.StringResource

/**
 * Platform speech-to-text bridge. Listens in the app's Indian language (`hi-IN`, `kn-IN`, …).
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
    data class Error(val message: StringResource, val needsPermission: Boolean = false) : SpeechEvent
    data object Ended : SpeechEvent
}

expect fun createSpeechToText(): SpeechToText
