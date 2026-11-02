@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.bhargav.pansariwala.voice

import kotlin.js.JsAny
import kotlin.js.js
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_speech_failed
import pansariwala.shared.generated.resources.error_speech_unavailable

private const val EVENT_STARTED = 0
private const val EVENT_PARTIAL = 1
private const val EVENT_FINAL = 2
private const val EVENT_ERROR = 3
private const val EVENT_DENIED = 4
private const val EVENT_ENDED = 5

/** Browser Web Speech API (`SpeechRecognition`, Chrome / Edge / Safari); keeps listening until stopped, like Android. */
private class WebSpeechToText : SpeechToText {
    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()
    private var handle: JsAny? = null

    override fun isAvailable(): Boolean = speechRecognitionSupported()

    override fun startListening() {
        if (!isAvailable()) {
            _events.tryEmit(SpeechEvent.Error(Res.string.error_speech_unavailable))
            _events.tryEmit(SpeechEvent.Ended)
            return
        }
        handle?.let(::recognitionStop)
        handle = recognitionStart(SPEECH_LANGUAGE) { kind, text ->
            val event = when (kind) {
                EVENT_STARTED -> SpeechEvent.Started
                EVENT_PARTIAL -> SpeechEvent.PartialResult(text)
                EVENT_FINAL -> SpeechEvent.FinalResult(text)
                EVENT_DENIED -> SpeechEvent.Error(Res.string.error_speech_failed, needsPermission = true)
                EVENT_ERROR -> SpeechEvent.Error(Res.string.error_speech_failed)
                else -> SpeechEvent.Ended
            }
            _events.tryEmit(event)
        }
    }

    override fun stopListening() {
        handle?.let(::recognitionStop)
        handle = null
    }

    override fun cancel() {
        handle?.let(::recognitionAbort)
        handle = null
    }

    private companion object {
        const val SPEECH_LANGUAGE = "hi-IN"
    }
}

actual fun createSpeechToText(): SpeechToText = WebSpeechToText()

internal fun speechRecognitionSupported(): Boolean =
    js("typeof window !== 'undefined' && !!(window.SpeechRecognition || window.webkitSpeechRecognition)")

private fun recognitionStop(handle: JsAny): Unit = js("handle.stop()")

private fun recognitionAbort(handle: JsAny): Unit = js("handle.abort()")

/** Starts recognition; restarts after the browser's silence timeout until [recognitionStop] / [recognitionAbort]. */
private fun recognitionStart(lang: String, onEvent: (Int, String) -> Unit): JsAny = js(
    """(() => {
  const R = window.SpeechRecognition || window.webkitSpeechRecognition;
  const h = { stopped: false, rec: null };
  const begin = () => {
    const rec = new R();
    rec.lang = lang;
    rec.continuous = true;
    rec.interimResults = true;
    rec.maxAlternatives = 1;
    rec.onstart = () => onEvent(0, '');
    rec.onresult = (e) => {
      let interim = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i];
        const t = r[0] ? r[0].transcript : '';
        if (r.isFinal) { if (t.trim()) onEvent(2, t.trim()); } else interim += t;
      }
      if (interim.trim()) onEvent(1, interim.trim());
    };
    rec.onerror = (e) => {
      if (e.error === 'not-allowed' || e.error === 'service-not-allowed') { h.stopped = true; onEvent(4, ''); }
      else if (e.error !== 'no-speech' && e.error !== 'aborted') onEvent(3, '');
    };
    rec.onend = () => {
      if (!h.stopped) { try { begin(); return; } catch (err) {} }
      onEvent(5, '');
    };
    h.rec = rec;
    rec.start();
  };
  h.stop = () => { h.stopped = true; try { h.rec && h.rec.stop(); } catch (err) {} };
  h.abort = () => { h.stopped = true; try { h.rec && h.rec.abort(); } catch (err) {} };
  try { begin(); } catch (err) { h.stopped = true; onEvent(3, ''); onEvent(5, ''); }
  return h;
})()"""
)
