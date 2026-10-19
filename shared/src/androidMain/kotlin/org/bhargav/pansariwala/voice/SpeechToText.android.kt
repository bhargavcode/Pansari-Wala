package org.bhargav.pansariwala.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.component.KoinComponent
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.component.inject

/** RecognizerIntent.EXTRA_ADDITIONAL_LANGUAGES (API 34); older recognisers ignore it. */
private const val EXTRA_ADDITIONAL_LANGUAGES = "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES"
private const val SILENCE_RESTART_MS = 150L
private const val MAX_RESTART_BACKOFF_MS = 3_000L
private const val BUSY_RECREATE_THRESHOLD = 2
private const val COMPLETE_SILENCE_MS = 1_500L
private const val SEGMENTED_SESSION_MS = 5 * 60_000L

private class AndroidSpeechToText(
    private val context: Context,
) : SpeechToText {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null
    private var continuous = false
    private var listening = false
    private var sessionActive = false
    private var lastPartial: String = ""
    private var committedThisUtterance = false
    private var restartScheduled = false
    private var consecutiveFailures = 0
    private var busyCount = 0

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening() {
        mainHandler.post {
            if (!isAvailable()) {
                _events.tryEmit(SpeechEvent.Error("Speech recognition is not available on this device."))
                return@post
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                _events.tryEmit(
                    SpeechEvent.Error(
                        message = "Microphone permission is required for voice orders.",
                        needsPermission = true,
                    ),
                )
                return@post
            }
            continuous = true
            sessionActive = false
            lastPartial = ""
            committedThisUtterance = false
            restartScheduled = false
            consecutiveFailures = 0
            busyCount = 0
            ensureRecognizer()
            beginListeningInternal()
        }
    }

    override fun stopListening() {
        mainHandler.post {
            continuous = false
            listening = false
            restartScheduled = false
            commitPartialIfNeeded()
            runCatching { recognizer?.stopListening() }
            sessionActive = false
            _events.tryEmit(SpeechEvent.Ended)
        }
    }

    override fun cancel() {
        mainHandler.post {
            continuous = false
            listening = false
            restartScheduled = false
            lastPartial = ""
            committedThisUtterance = true
            runCatching { recognizer?.cancel() }
            sessionActive = false
            _events.tryEmit(SpeechEvent.Ended)
        }
    }

    private fun commitPartialIfNeeded() {
        val text = lastPartial.trim()
        if (!committedThisUtterance && text.isNotBlank()) {
            committedThisUtterance = true
            _events.tryEmit(SpeechEvent.FinalResult(text))
        }
        lastPartial = ""
    }

    /** Grows the restart delay while the recognizer keeps failing so the mic doesn't flap on/off. */
    private fun backoffDelay(baseMs: Long): Long {
        consecutiveFailures++
        return (baseMs * consecutiveFailures).coerceAtMost(MAX_RESTART_BACKOFF_MS)
    }

    private fun scheduleRestart(delayMs: Long = 300) {
        if (!continuous || restartScheduled) return
        restartScheduled = true
        mainHandler.postDelayed({
            restartScheduled = false
            if (continuous) beginListeningInternal()
        }, delayMs)
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        createRecognizer()
    }

    private fun recreateRecognizer() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        listening = false
        createRecognizer()
    }

    private fun createRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    committedThisUtterance = false
                    // Emit Started only once per mic session so commit-dedup state is kept.
                    if (!sessionActive) {
                        sessionActive = true
                        _events.tryEmit(SpeechEvent.Started)
                    }
                }

                override fun onBeginningOfSpeech() {
                    committedThisUtterance = false
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    listening = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        -> {
                            // Silence / background noise only: reopen quickly so speech isn't missed.
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(SILENCE_RESTART_MS) else endSession()
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(backoffDelay(250)) else endSession()
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            // Cancel the in-flight session first; only rebuild the recognizer if busy persists.
                            if (continuous) {
                                busyCount++
                                if (busyCount >= BUSY_RECREATE_THRESHOLD) {
                                    busyCount = 0
                                    recreateRecognizer()
                                } else {
                                    runCatching { recognizer?.cancel() }
                                }
                                scheduleRestart(backoffDelay(300))
                            } else {
                                endSession()
                            }
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            continuous = false
                            sessionActive = false
                            _events.tryEmit(
                                SpeechEvent.Error(
                                    message = "Microphone permission is required for voice orders.",
                                    needsPermission = true,
                                ),
                            )
                            _events.tryEmit(SpeechEvent.Ended)
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_SERVER,
                        -> {
                            // Soft retry — don't dump raw errors into the order UI.
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(backoffDelay(700)) else endSession()
                        }
                        else -> {
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(backoffDelay(400)) else endSession()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    emitFinal(results)
                    if (continuous) scheduleRestart(SILENCE_RESTART_MS) else endSession()
                }

                // Android 13+ segmented session: one result per phrase while the mic stays open.
                override fun onSegmentResults(segmentResults: Bundle) {
                    emitFinal(segmentResults)
                    committedThisUtterance = false
                }

                override fun onEndOfSegmentedSession() {
                    listening = false
                    commitPartialIfNeeded()
                    if (continuous) scheduleRestart(SILENCE_RESTART_MS) else endSession()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) {
                        consecutiveFailures = 0
                        busyCount = 0
                        lastPartial = text
                        _events.tryEmit(SpeechEvent.PartialResult(text))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun emitFinal(results: Bundle?) {
        val best = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
            .ifBlank { lastPartial }
        if (best.isNotBlank() && !committedThisUtterance) {
            committedThisUtterance = true
            consecutiveFailures = 0
            _events.tryEmit(SpeechEvent.FinalResult(best))
        }
        lastPartial = ""
    }

    private fun endSession() {
        continuous = false
        sessionActive = false
        _events.tryEmit(SpeechEvent.Ended)
    }

    private fun beginListeningInternal() {
        val sr = recognizer ?: return
        if (listening) return
        lastPartial = ""
        committedThisUtterance = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConstants.Voice.PRIMARY_LOCALE)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, AppConstants.Voice.PRIMARY_LOCALE)
            putExtra(EXTRA_ADDITIONAL_LANGUAGES, arrayOf(AppConstants.Voice.SECONDARY_LOCALE))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Tolerate short pauses between "1 kilo ... chini" instead of ending the session.
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                COMPLETE_SILENCE_MS,
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                COMPLETE_SILENCE_MS,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, true)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    SEGMENTED_SESSION_MS,
                )
            }
        }
        runCatching {
            sr.startListening(intent)
        }.onFailure {
            if (continuous) {
                recreateRecognizer()
                scheduleRestart(500)
            } else {
                sessionActive = false
                _events.tryEmit(SpeechEvent.Error("Failed to start listening. Tap the mic and try again."))
                _events.tryEmit(SpeechEvent.Ended)
            }
        }
    }
}

actual fun createSpeechToText(): SpeechToText {
    val holder = object : KoinComponent {
        val context: Context by inject()
    }
    return AndroidSpeechToText(holder.context.applicationContext)
}
