package org.bhargav.pansariwala.voice

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

private class IosTextToSpeech : TextToSpeech {
    private val synthesizer = AVSpeechSynthesizer()

    override fun isAvailable(): Boolean = true

    override fun speak(text: String, languageTag: String) {
        if (text.isBlank()) return
        stop()
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)?.let { utterance.voice = it }
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        if (synthesizer.isSpeaking()) synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

actual fun createTextToSpeech(): TextToSpeech = IosTextToSpeech()
