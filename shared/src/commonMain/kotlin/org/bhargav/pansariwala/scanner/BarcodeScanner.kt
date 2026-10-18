package org.bhargav.pansariwala.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

@Serializable
enum class ScannerLens { BACK, FRONT }

/** Device-local camera options for the POS barcode scanner; applied only after the user saves them. */
@Serializable
data class ScannerSettings(
    val lens: ScannerLens = ScannerLens.BACK,
    val torch: Boolean = false,
    val beepOnScan: Boolean = true,
    val vibrateOnScan: Boolean = true,
    /** Retail codes only (EAN/UPC/Code128) — far cheaper per frame than scanning every symbology. */
    val retailFormatsOnly: Boolean = true,
    /** Ignore the same code for this long so one product held in view is not added repeatedly. */
    val repeatDelayMs: Long = DEFAULT_REPEAT_DELAY_MS,
) {
    fun switchLens(): ScannerSettings = copy(lens = if (lens == ScannerLens.BACK) ScannerLens.FRONT else ScannerLens.BACK)

    companion object {
        const val DEFAULT_REPEAT_DELAY_MS: Long = 1_500L
        val REPEAT_DELAY_OPTIONS_MS: List<Long> = listOf(800L, 1_500L, 3_000L)
    }
}

/** False on web targets where no camera pipeline is wired. */
expect val barcodeScannerSupported: Boolean

/**
 * Live camera preview that reports decoded barcodes. The camera only runs while [active] is true and the
 * composable is in composition; analysis drops frames it cannot keep up with.
 */
@Composable
expect fun BarcodeScannerPreview(
    settings: ScannerSettings,
    active: Boolean,
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
)

/** Short confirmation (sound / haptic) after a successful scan. */
expect fun scanFeedback(beep: Boolean, vibrate: Boolean)
