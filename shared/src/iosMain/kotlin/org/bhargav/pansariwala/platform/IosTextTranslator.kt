package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.i18n.AppLanguage
import kotlin.coroutines.resume

/** Implemented in Swift with Apple's Translation framework (on-device). */
interface IosTranslatorHost {
    /** Calls back with the subset of [languageCodes] English can be translated into. */
    fun supportedLanguages(languageCodes: List<String>, onResult: (List<String>) -> Unit)

    /** Calls back with English [text] in [languageCode], or null on failure. */
    fun translate(text: String, languageCode: String, onResult: (String?) -> Unit)
}

object IosTranslatorBridge {
    var host: IosTranslatorHost? = null
}

class IosTextTranslator : TextTranslator {
    override suspend fun supportedLanguages(): Set<String> {
        val host = IosTranslatorBridge.host ?: return emptySet()
        val codes = AppLanguage.entries.filter { it != AppLanguage.ENGLISH }.map { it.code }
        return suspendCancellableCoroutine { cont ->
            host.supportedLanguages(codes) { supported -> if (cont.isActive) cont.resume(supported.toSet()) }
        }
    }

    override suspend fun translate(texts: List<String>, languageCode: String): List<String>? {
        val host = IosTranslatorBridge.host
        return texts.map { text ->
            if (text.isBlank()) return@map ""
            host ?: return null
            suspendCancellableCoroutine<String?> { cont ->
                host.translate(text.trim(), languageCode) { result -> if (cont.isActive) cont.resume(result) }
            } ?: return null
        }
    }
}
