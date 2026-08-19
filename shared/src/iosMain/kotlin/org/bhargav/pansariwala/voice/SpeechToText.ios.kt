package org.bhargav.pansariwala.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.AVFAudio.setPreferredSampleRate
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

private const val NSEC_PER_MSEC: Long = 1_000_000L

/**
 * iOS SFSpeechRecognizer bridge for hi-IN.
 *
 * Important: never restart recognition synchronously inside the recognition
 * callback — that triggers kAFAssistantErrorDomain error 216. Restarts are
 * always deferred to the next main-queue turn after teardown.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosSpeechToText : SpeechToText {

    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()

    private var audioEngine = AVAudioEngine()
    private val recognizer: SFSpeechRecognizer =
        SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "hi-IN"))
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: SFSpeechRecognitionTask? = null
    private var continuous = false
    private var sessionActive = false
    private var lastPartial: String = ""
    private var committedThisUtterance = false
    private var intentionalStop = false
    private var tapInstalled = false

    override fun isAvailable(): Boolean = recognizer.isAvailable()

    override fun startListening() {
        intentionalStop = false
        SFSpeechRecognizer.requestAuthorization { status ->
            // SFSpeechRecognizerAuthorizationStatusAuthorized == 3
            if (status.value == 3L) {
                continuous = true
                sessionActive = false
                lastPartial = ""
                committedThisUtterance = false
                startEngine()
            } else {
                _events.tryEmit(
                    SpeechEvent.Error(
                        message = "Speech / microphone permission is required for voice orders.",
                        needsPermission = true,
                    ),
                )
            }
        }
    }

    private fun startEngine() {
        if (!continuous || intentionalStop) return
        if (!isAvailable()) {
            _events.tryEmit(SpeechEvent.Error("Speech recognition is not available on this device."))
            return
        }

        teardownEngine()

        val sessionReady = runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                AVAudioSessionCategoryPlayAndRecord,
                withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
                error = null,
            )
            session.setMode(AVAudioSessionModeMeasurement, error = null)
            // Prefer a known-valid rate so inputNode format is not 0/0 after restart.
            session.setPreferredSampleRate(44_100.0, error = null)
            session.setActive(true, error = null)
            true
        }.getOrDefault(false)
        if (!sessionReady) {
            scheduleRestart(delayMs = 500)
            return
        }

        // Fresh engine avoids stale zero sample-rate format after rapid teardown/restart.
        audioEngine = AVAudioEngine()

        val recognitionRequest = SFSpeechAudioBufferRecognitionRequest().also {
            it.shouldReportPartialResults = true
            // Prefer on-device when available — fewer network / assistant errors.
            runCatching { it.setRequiresOnDeviceRecognition(true) }
        }
        request = recognitionRequest
        lastPartial = ""
        committedThisUtterance = false

        val inputNode = audioEngine.inputNode
        // Prefer hardware input format; fall back to output bus format.
        val inputFormat = inputNode.inputFormatForBus(0u)
        val outputFormat = inputNode.outputFormatForBus(0u)
        val format = when {
            isValidAudioFormat(inputFormat) -> inputFormat
            isValidAudioFormat(outputFormat) -> outputFormat
            else -> null
        }
        // installTap asserts IsFormatSampleRateAndChannelCountValid — never call with 0 rate/channels.
        if (format == null) {
            scheduleRestart(delayMs = 500)
            return
        }

        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            if (buffer != null) {
                recognitionRequest.appendAudioPCMBuffer(buffer)
            }
        }
        tapInstalled = true

        audioEngine.prepare()
        val ok = audioEngine.startAndReturnError(null)
        if (!ok) {
            scheduleRestart(delayMs = 500)
            return
        }

        if (!sessionActive) {
            sessionActive = true
            _events.tryEmit(SpeechEvent.Started)
        }

        task = recognizer.recognitionTaskWithRequest(recognitionRequest) { result, error: NSError? ->
            if (intentionalStop) return@recognitionTaskWithRequest

            val text = result?.bestTranscription?.formattedString.orEmpty()
            if (text.isNotBlank()) {
                lastPartial = text
                val isFinal = result?.isFinal() == true
                if (isFinal) {
                    if (!committedThisUtterance) {
                        committedThisUtterance = true
                        _events.tryEmit(SpeechEvent.FinalResult(text))
                    }
                    if (continuous) {
                        // Defer restart — sync restart inside this callback causes error 216.
                        scheduleRestart(delayMs = 350)
                    }
                } else {
                    _events.tryEmit(SpeechEvent.PartialResult(text))
                }
            }

            if (error != null) {
                handleRecognitionError(error)
            }
        }
    }

    private fun handleRecognitionError(error: NSError) {
        val code = error.code
        // 203 = cancelled, 209 = no speech, 216 = request was canceled / restarted,
        // 1110 = no speech detected. These are normal during continuous POS use.
        val recoverable = code == 203L || code == 209L || code == 216L ||
            code == 1110L || code == 1L || intentionalStop

        if (recoverable) {
            commitPartialIfNeeded()
            if (continuous && !intentionalStop) {
                scheduleRestart(delayMs = 400)
            }
            return
        }

        if (continuous && !intentionalStop) {
            // Soft failure — keep listening without showing a raw system error.
            commitPartialIfNeeded()
            scheduleRestart(delayMs = 600)
            return
        }

        continuous = false
        sessionActive = false
        _events.tryEmit(SpeechEvent.Error("Speech recognition failed. Tap the mic and try again."))
        teardownEngine()
        _events.tryEmit(SpeechEvent.Ended)
    }

    private fun commitPartialIfNeeded() {
        val text = lastPartial.trim()
        if (!committedThisUtterance && text.isNotBlank()) {
            committedThisUtterance = true
            _events.tryEmit(SpeechEvent.FinalResult(text))
        }
        lastPartial = ""
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!continuous || intentionalStop) return
        val whenNs = dispatch_time(DISPATCH_TIME_NOW, delayMs * NSEC_PER_MSEC)
        dispatch_after(whenNs, dispatch_get_main_queue()) {
            if (continuous && !intentionalStop) {
                startEngine()
            }
        }
    }

    override fun stopListening() {
        intentionalStop = true
        continuous = false
        commitPartialIfNeeded()
        request?.endAudio()
        teardownEngine()
        sessionActive = false
        _events.tryEmit(SpeechEvent.Ended)
    }

    override fun cancel() {
        intentionalStop = true
        continuous = false
        lastPartial = ""
        committedThisUtterance = true
        runCatching { task?.cancel() }
        teardownEngine()
        sessionActive = false
        _events.tryEmit(SpeechEvent.Ended)
    }

    private fun teardownEngine() {
        runCatching {
            if (audioEngine.running) {
                audioEngine.stop()
            }
            if (tapInstalled) {
                audioEngine.inputNode.removeTapOnBus(0u)
                tapInstalled = false
            }
        }
        runCatching { request?.endAudio() }
        request = null
        task = null
    }

    private fun isValidAudioFormat(format: AVAudioFormat): Boolean =
        format.sampleRate > 0.0 && format.channelCount.toInt() > 0
}

actual fun createSpeechToText(): SpeechToText = IosSpeechToText()
