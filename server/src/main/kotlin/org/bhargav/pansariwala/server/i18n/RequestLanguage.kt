package org.bhargav.pansariwala.server.i18n

import kotlinx.coroutines.asContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Language of the app that made the current request, read from [HEADER] (the POS, user and delivery apps
 * send their selected language). The master console never sends it, so it always edits English source data.
 * Bound per call as a coroutine context element, so blocking store code can read [current] directly.
 */
object RequestLanguage {
    const val HEADER = "X-App-Language"
    const val ENGLISH = "en"
    const val HINDI = "hi"

    /** Translated languages; mirrors the app's `AppLanguage` codes (English is the source text). */
    val TRANSLATED: Set<String> = setOf(HINDI, "kn", "pa", "mr", "bn")

    private val local = ThreadLocal<String>()

    fun current(): String = local.get() ?: ENGLISH

    fun element(code: String?): CoroutineContext = local.asContextElement(normalize(code))

    fun normalize(code: String?): String =
        code?.trim()?.lowercase()?.substringBefore(',')?.substringBefore('-')?.substringBefore('_')
            ?.takeIf { it in TRANSLATED } ?: ENGLISH
}

/** [english] in [lang] from this lang → text map; blank or missing translations fall back to English. */
fun Map<String, String>.localized(english: String, lang: String = RequestLanguage.current()): String =
    if (lang == RequestLanguage.ENGLISH) english else this[lang]?.takeIf { it.isNotBlank() } ?: english
