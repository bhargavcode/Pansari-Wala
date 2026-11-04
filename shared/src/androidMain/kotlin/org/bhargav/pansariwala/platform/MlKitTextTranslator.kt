package org.bhargav.pansariwala.platform

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bhargav.pansariwala.i18n.AppLanguage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit on-device translation. Each language model (~30 MB) downloads once on first use,
 * which needs internet; after that translation works offline.
 */
class MlKitTextTranslator : TextTranslator {
    private val mutex = Mutex()
    private val translators = mutableMapOf<String, Translator>()

    private val supported: Set<String> = AppLanguage.entries
        .filter { it != AppLanguage.ENGLISH && TranslateLanguage.fromLanguageTag(it.code) != null }
        .map { it.code }
        .toSet()

    override suspend fun supportedLanguages(): Set<String> = supported

    override suspend fun translate(text: String, languageCode: String): String? {
        if (text.isBlank()) return ""
        val target = TranslateLanguage.fromLanguageTag(languageCode) ?: return null
        val translator = mutex.withLock {
            translators.getOrPut(target) {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(target)
                        .build(),
                )
            }
        }
        return try {
            translator.downloadModelIfNeeded().await()
            translator.translate(text.trim()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
