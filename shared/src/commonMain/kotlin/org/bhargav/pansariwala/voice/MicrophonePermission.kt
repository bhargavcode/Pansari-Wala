package org.bhargav.pansariwala.voice

import androidx.compose.runtime.Composable

/**
 * Platform mic permission gate used before starting STT.
 * [onResult] is invoked with true when recording may proceed.
 */
@Composable
expect fun RequestMicrophonePermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
)
