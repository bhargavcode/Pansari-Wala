package org.bhargav.pansariwala.platform

/** Translates English text on the device; no text is sent to a server. */
interface TextTranslator {
    /** App language codes English can be translated into on this device. */
    suspend fun supportedLanguages(): Set<String>

    /** English [text] in [languageCode], or null when unsupported or the language model is unavailable. */
    suspend fun translate(text: String, languageCode: String): String?
}

/** Platforms without an on-device translator. */
object NoTextTranslator : TextTranslator {
    override suspend fun supportedLanguages(): Set<String> = emptySet()
    override suspend fun translate(text: String, languageCode: String): String? = null
}
