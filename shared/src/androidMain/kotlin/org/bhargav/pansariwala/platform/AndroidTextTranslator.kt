package org.bhargav.pansariwala.platform

import android.content.Context
import android.icu.util.ULocale
import android.os.Build
import android.os.CancellationSignal
import android.view.translation.TranslationCapability
import android.view.translation.TranslationContext
import android.view.translation.TranslationManager
import android.view.translation.TranslationRequest
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponse
import android.view.translation.TranslationSpec
import android.view.translation.Translator
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.i18n.AppLanguage
import kotlin.coroutines.resume

/**
 * Android's built-in on-device translator (Android 12+, no extra library). Which languages exist
 * depends on the phone's system translation service; older or unsupported phones get none.
 */
class AndroidTextTranslator(context: Context) : TextTranslator {
    private val system: SystemTranslator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SystemTranslator(context) else null

    override suspend fun supportedLanguages(): Set<String> = system?.supportedLanguages().orEmpty()

    override suspend fun translate(texts: List<String>, languageCode: String): List<String>? {
        if (texts.all { it.isBlank() }) return texts.map { "" }
        return system?.translate(texts, languageCode)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class SystemTranslator(context: Context) {
    private val manager: TranslationManager? = context.getSystemService(TranslationManager::class.java)
    private val mutex = Mutex()
    private val translators = mutableMapOf<String, Translator>()

    @Volatile
    private var supported: Set<String>? = null

    suspend fun supportedLanguages(): Set<String> {
        supported?.let { return it }
        val manager = manager ?: return emptySet()
        val capabilities = withContext(Dispatchers.IO) {
            runCatching {
                manager.getOnDeviceTranslationCapabilities(TranslationSpec.DATA_FORMAT_TEXT, TranslationSpec.DATA_FORMAT_TEXT)
            }.getOrDefault(emptySet())
        }
        val targets = capabilities
            .filter {
                it.sourceSpec.locale.language == SOURCE_LANGUAGE &&
                    it.state != TranslationCapability.STATE_NOT_AVAILABLE
            }
            .map { it.targetSpec.locale.language }
            .toSet()
        return AppLanguage.entries
            .filter { it != AppLanguage.ENGLISH && it.code in targets }
            .map { it.code }
            .toSet()
            .also { supported = it }
    }

    suspend fun translate(texts: List<String>, languageCode: String): List<String>? {
        val filled = texts.indices.filter { texts[it].isNotBlank() }
        val translator = translatorFor(languageCode) ?: return null
        val request = TranslationRequest.Builder()
            .setTranslationRequestValues(filled.map { TranslationRequestValue.forText(texts[it].trim()) })
            .build()
        val signal = CancellationSignal()
        val translated: List<String>? = suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { signal.cancel() }
            try {
                translator.translate(request, signal, Runnable::run) { response ->
                    val values = response.translationResponseValues
                    val out = filled.indices.map { values.get(it)?.text?.toString() }
                    val ok = response.translationStatus == TranslationResponse.TRANSLATION_STATUS_SUCCESS &&
                        out.none { it.isNullOrBlank() }
                    if (cont.isActive) cont.resume(if (ok) out.map { it.orEmpty() } else null)
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
        translated ?: return null
        return texts.indices.map { i -> filled.indexOf(i).takeIf { it >= 0 }?.let { translated[it] } ?: "" }
    }

    /** One cached translator per language; creating one binds the system service, so do it once. */
    private suspend fun translatorFor(languageCode: String): Translator? = mutex.withLock {
        translators[languageCode]?.takeUnless { it.isDestroyed }?.let { return@withLock it }
        val manager = manager ?: return@withLock null
        val context = TranslationContext.Builder(
            TranslationSpec(ULocale.ENGLISH, TranslationSpec.DATA_FORMAT_TEXT),
            TranslationSpec(ULocale(languageCode), TranslationSpec.DATA_FORMAT_TEXT),
        ).build()
        val created = withTimeoutOrNull(CREATE_TIMEOUT_MS) {
            suspendCancellableCoroutine<Translator?> { cont ->
                try {
                    manager.createOnDeviceTranslator(context, Runnable::run) { translator ->
                        if (cont.isActive) cont.resume(translator)
                    }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        created?.also { translators[languageCode] = it }
    }

    private companion object {
        const val SOURCE_LANGUAGE = "en"
        const val CREATE_TIMEOUT_MS = 10_000L
    }
}
