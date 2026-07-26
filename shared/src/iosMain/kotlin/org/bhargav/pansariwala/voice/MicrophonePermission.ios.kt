package org.bhargav.pansariwala.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.AVFAudio.AVAudioSession
import platform.Speech.SFSpeechRecognizer

@Composable
actual fun RequestMicrophonePermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        onConsumed()
        SFSpeechRecognizer.requestAuthorization { speechStatus ->
            // SFSpeechRecognizerAuthorizationStatusAuthorized == 3
            if (speechStatus.value != 3L) {
                onResult(false)
                return@requestAuthorization
            }
            AVAudioSession.sharedInstance().requestRecordPermission { micGranted ->
                onResult(micGranted)
            }
        }
    }
}
