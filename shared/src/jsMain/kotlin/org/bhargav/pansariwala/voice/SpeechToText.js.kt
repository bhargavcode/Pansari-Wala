package org.bhargav.pansariwala.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_speech_unavailable

private class UnsupportedSpeechToText : SpeechToText {
    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()

    override fun isAvailable(): Boolean = false

    override fun startListening() {
        _events.tryEmit(
            SpeechEvent.Error(Res.string.error_speech_unavailable),
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
