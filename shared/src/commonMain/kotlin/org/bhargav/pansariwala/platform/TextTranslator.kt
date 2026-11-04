package org.bhargav.pansariwala.platform

/** Translates English text with the OS's on-device translator; no text is sent to our servers. */
interface TextTranslator {
    /** App language codes English can be translated into on this device. */
    suspend fun supportedLanguages(): Set<String>

    /**
     * English [texts] in [languageCode], in the same order (blank stays blank),
     * or null when unsupported or the language isn't available on the device.
     */
    suspend fun translate(texts: List<String>, languageCode: String): List<String>?
}

/** Platforms without an on-device translator. */
object NoTextTranslator : TextTranslator {
    override suspend fun supportedLanguages(): Set<String> = emptySet()
    override suspend fun translate(texts: List<String>, languageCode: String): List<String>? = null
}
