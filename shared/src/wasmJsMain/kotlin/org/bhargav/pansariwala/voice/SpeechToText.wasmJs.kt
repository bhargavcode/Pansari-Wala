package org.bhargav.pansariwala.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private class UnsupportedSpeechToText : SpeechToText {
    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()

    override fun isAvailable(): Boolean = false

    override fun startListening() {
        _events.tryEmit(
            SpeechEvent.Error("Speech recognition is not supported on web in Phase 3."),
        )
        _events.tryEmit(SpeechEvent.Ended)
    }

    override fun stopListening() {
        _events.tryEmit(SpeechEvent.Ended)
    }

    override fun cancel() {
        _events.tryEmit(SpeechEvent.Ended)
    }
}

actual fun createSpeechToText(): SpeechToText = UnsupportedSpeechToText()
