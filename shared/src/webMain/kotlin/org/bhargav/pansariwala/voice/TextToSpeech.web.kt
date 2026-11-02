@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.bhargav.pansariwala.voice

import kotlin.js.js

/** Browser `speechSynthesis`; picks a voice for the language when one is installed. */
private object WebTextToSpeech : TextToSpeech {
    override fun isAvailable(): Boolean = speechSynthesisSupported()

    override fun speak(text: String, languageTag: String) {
        if (text.isBlank() || !isAvailable()) return
        synthSpeak(text, languageTag)
    }

    override fun stop() {
        if (isAvailable()) synthCancel()
    }
}

actual fun createTextToSpeech(): TextToSpeech = WebTextToSpeech

private fun speechSynthesisSupported(): Boolean =
    js("typeof window !== 'undefined' && !!window.speechSynthesis && typeof SpeechSynthesisUtterance !== 'undefined'")

private fun synthCancel(): Unit = js("window.speechSynthesis.cancel()")

private fun synthSpeak(text: String, lang: String): Unit = js(
    """(() => {
  const s = window.speechSynthesis;
  s.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = lang;
  const base = lang.split('-')[0].toLowerCase();
  const voices = s.getVoices() || [];
  const voice = voices.find(v => v.lang && v.lang.toLowerCase() === lang.toLowerCase())
    || voices.find(v => v.lang && v.lang.toLowerCase().startsWith(base));
  if (voice) u.voice = voice;
  s.speak(u);
})()"""
)
