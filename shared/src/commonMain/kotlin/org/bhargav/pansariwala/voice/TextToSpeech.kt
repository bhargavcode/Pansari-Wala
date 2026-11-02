package org.bhargav.pansariwala.voice

/** Platform text-to-speech used to read voice-search answers aloud. */
interface TextToSpeech {
    fun isAvailable(): Boolean

    /** Speaks [text], replacing anything still being spoken. [languageTag] is BCP-47, e.g. `hi-IN`. */
    fun speak(text: String, languageTag: String)

    fun stop()
}

expect fun createTextToSpeech(): TextToSpeech
