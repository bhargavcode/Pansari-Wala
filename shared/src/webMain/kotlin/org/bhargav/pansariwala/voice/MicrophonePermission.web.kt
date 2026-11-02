package org.bhargav.pansariwala.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** The browser shows its own mic prompt when recognition starts; a refusal arrives as a speech error. */
@Composable
actual fun RequestMicrophonePermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        onConsumed()
        onResult(speechRecognitionSupported())
    }
}
