package org.bhargav.pansariwala.voice

import android.content.Context
import java.util.Locale
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.speech.tts.TextToSpeech as AndroidTts

private class AndroidTextToSpeech(private val context: Context) : TextToSpeech {
    private var engine: AndroidTts? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    private fun ensureEngine(): AndroidTts = engine ?: AndroidTts(context) { status ->
        ready = status == AndroidTts.SUCCESS
        pending?.let { (text, tag) -> if (ready) say(text, tag) }
        pending = null
    }.also { engine = it }

    override fun isAvailable(): Boolean = true

    override fun speak(text: String, languageTag: String) {
        if (text.isBlank()) return
        ensureEngine()
        if (ready) say(text, languageTag) else pending = text to languageTag
    }

    private fun say(text: String, languageTag: String) {
        val tts = engine ?: return
        val locale = Locale.forLanguageTag(languageTag)
        if (tts.isLanguageAvailable(locale) >= AndroidTts.LANG_AVAILABLE) tts.language = locale
        tts.speak(text, AndroidTts.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        pending = null
        engine?.stop()
    }

    private companion object {
        const val UTTERANCE_ID = "pansari_voice_answer"
    }
}

actual fun createTextToSpeech(): TextToSpeech {
    val holder = object : KoinComponent {
        val context: Context by inject()
    }
    return AndroidTextToSpeech(holder.context.applicationContext)
}
