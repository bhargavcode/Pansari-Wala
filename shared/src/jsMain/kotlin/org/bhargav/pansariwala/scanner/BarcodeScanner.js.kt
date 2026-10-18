package org.bhargav.pansariwala.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val barcodeScannerSupported: Boolean = false

@Composable
actual fun BarcodeScannerPreview(
    settings: ScannerSettings,
    active: Boolean,
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) = Unit

actual fun scanFeedback(beep: Boolean, vibrate: Boolean) = Unit
