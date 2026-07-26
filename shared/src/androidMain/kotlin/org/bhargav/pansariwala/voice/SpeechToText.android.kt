package org.bhargav.pansariwala.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import org.koin.core.component.inject

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
                        SpeechRecognizer.ERROR_CLIENT,
                        -> {
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(250) else endSession()
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            // Recreate and retry quietly — common during continuous restarts.
                            if (continuous) {
                                recreateRecognizer()
                                scheduleRestart(400)
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
                            if (continuous) scheduleRestart(700) else endSession()
                        }
                        else -> {
                            commitPartialIfNeeded()
                            if (continuous) scheduleRestart(400) else endSession()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val texts = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    val best = texts.firstOrNull().orEmpty().ifBlank { lastPartial }
                    if (best.isNotBlank() && !committedThisUtterance) {
                        committedThisUtterance = true
                        lastPartial = ""
                        _events.tryEmit(SpeechEvent.FinalResult(best))
                    } else {
                        lastPartial = ""
                    }
                    if (continuous) scheduleRestart(250) else endSession()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) {
                        lastPartial = text
                        _events.tryEmit(SpeechEvent.PartialResult(text))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
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
