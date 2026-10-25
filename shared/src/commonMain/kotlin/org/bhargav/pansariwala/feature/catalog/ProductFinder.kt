package org.bhargav.pansariwala.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.feature.order.ListeningMicButton
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.options
import org.bhargav.pansariwala.feature.user.toOptionEntries
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.scanner.BarcodeScannerPreview
import org.bhargav.pansariwala.scanner.ScannerSettings
import org.bhargav.pansariwala.scanner.barcodeScannerSupported
import org.bhargav.pansariwala.scanner.scanFeedback
import org.bhargav.pansariwala.voice.CatalogMatchKind
import org.bhargav.pansariwala.voice.ProductFuzzyMatcher
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.bhargav.pansariwala.voice.SpeechEvent
import org.bhargav.pansariwala.voice.SpeechToText
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_mic_permission
import pansariwala.shared.generated.resources.error_speech_unavailable
import pansariwala.shared.generated.resources.finder_no_barcode_match
import pansariwala.shared.generated.resources.finder_no_match
import pansariwala.shared.generated.resources.finder_scan_cd
import pansariwala.shared.generated.resources.finder_scan_hint
import pansariwala.shared.generated.resources.finder_voice_hint

data class FinderResults(
    val kind: CatalogMatchKind,
    val label: String,
    val options: List<PurchaseOption>,
    val visibleCount: Int = FINDER_PAGE,
) {
    val visible: List<PurchaseOption> get() = options.take(visibleCount)
    val hasMore: Boolean get() = visibleCount < options.size
}

data class ProductFinderState(
    val listening: Boolean = false,
    val partial: String = "",
    val requestMic: Boolean = false,
    val scanning: Boolean = false,
    val results: FinderResults? = null,
    val message: UiText? = null,
)

/**
 * Find a product by voice (name, brand or category) or barcode (camera, or a USB/Bluetooth scanner typing
 * into the search field and pressing Enter). Matches land in [ProductFinderState.results] for the screen
 * to show in [MatchResultsPanel]; picking one runs that screen's action.
 */
class ProductFinder(
    private val speech: SpeechToText,
    private val scope: CoroutineScope,
    private val catalog: suspend () -> List<Product>,
) {
    private val _state = MutableStateFlow(ProductFinderState())
    val state: StateFlow<ProductFinderState> = _state.asStateFlow()
    private var micActive = false

    init {
        scope.launch { speech.events.collect { if (micActive) onSpeech(it) } }
    }

    fun onMicClick() {
        if (_state.value.listening) return stopListening()
        if (!speech.isAvailable()) return _state.update { it.copy(message = UiText.res(Res.string.error_speech_unavailable)) }
        _state.update { it.copy(requestMic = true, message = null) }
    }

    fun onMicPermission(granted: Boolean) {
        _state.update { it.copy(requestMic = false) }
        if (!granted) return _state.update { it.copy(message = UiText.res(Res.string.error_mic_permission)) }
        micActive = true
        _state.update { it.copy(listening = true, partial = "", message = null) }
        speech.startListening()
    }

    /** Screen left or mic tapped again: always releases the recognizer. */
    fun stopListening() {
        if (micActive) {
            micActive = false
            speech.cancel()
        }
        _state.update { it.copy(listening = false, partial = "") }
    }

    private fun onSpeech(event: SpeechEvent) {
        when (event) {
            SpeechEvent.Started -> _state.update { it.copy(listening = true) }
            is SpeechEvent.PartialResult -> _state.update { it.copy(partial = event.text) }
            is SpeechEvent.FinalResult -> {
                stopListening()
                search(event.text)
            }
            is SpeechEvent.Error -> {
                stopListening()
                _state.update { it.copy(message = UiText.res(event.message)) }
            }
            SpeechEvent.Ended -> {
                val pending = _state.value.partial
                stopListening()
                if (pending.isNotBlank()) search(pending)
            }
        }
    }

    fun openScanner() = _state.update { it.copy(scanning = true, message = null) }

    fun closeScanner() = _state.update { it.copy(scanning = false) }

    /** Camera scan or a hardware scanner's Enter in the search field. */
    fun onBarcode(raw: String) {
        val code = raw.trim()
        if (code.isEmpty()) return
        _state.update { it.copy(scanning = false) }
        scope.launch {
            val hits = catalog().flatMap { p ->
                when {
                    p.barcode == code -> p.options()
                    else -> p.options().filter { it.variant?.barcode == code }
                }
            }
            _state.update {
                if (hits.isEmpty()) it.copy(results = null, message = UiText.res(Res.string.finder_no_barcode_match, code))
                else it.copy(results = FinderResults(CatalogMatchKind.PRODUCT, code, hits), message = null)
            }
        }
    }

    fun search(text: String) {
        val query = text.trim()
        if (query.isEmpty()) return
        scope.launch {
            val found = ProductFuzzyMatcher.search(query, catalog())
            _state.update {
                if (found == null) it.copy(results = null, message = UiText.res(Res.string.finder_no_match, query))
                else it.copy(results = FinderResults(found.kind, found.label, found.products.toOptionEntries()), message = null)
            }
        }
    }

    fun loadMore() = _state.update { s -> s.copy(results = s.results?.let { it.copy(visibleCount = it.visibleCount + FINDER_PAGE) }) }

    fun dismissResults() = _state.update { it.copy(results = null, message = null) }
}

const val FINDER_PAGE = 20

/** USB/Bluetooth scanners type EAN/UPC digits; names never look like this. */
fun looksLikeBarcode(text: String): Boolean = text.length >= MIN_BARCODE_LENGTH && text.all { it.isLetterOrDigit() } && text.count { it.isDigit() } >= MIN_BARCODE_LENGTH

private const val MIN_BARCODE_LENGTH = 6

/** Mic + scan buttons for a search field's trailing slot. */
@Composable
fun ProductFinderActions(finder: ProductFinder, cameraEnabled: Boolean = barcodeScannerSupported) {
    val state by finder.state.collectAsStateWithLifecycle()
    RequestMicrophonePermission(
        trigger = state.requestMic,
        onConsumed = {},
        onResult = finder::onMicPermission,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (cameraEnabled) {
            IconButton(onClick = finder::openScanner) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(Res.string.finder_scan_cd))
            }
        }
        ListeningMicButton(isListening = state.listening, onClick = finder::onMicClick, modifier = Modifier.size(40.dp))
    }
}

/** Live transcript, "no match" note, the scanner sheet and the bottom results list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFinderResults(
    finder: ProductFinder,
    onPick: (PurchaseOption) -> Unit,
    modifier: Modifier = Modifier,
    scannerSettings: ScannerSettings = ScannerSettings(),
) {
    val state by finder.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.listening) {
            Text(
                state.partial.ifBlank { stringResource(Res.string.finder_voice_hint) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        state.message?.let {
            Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        state.results?.let { results ->
            MatchResultsPanel(
                kind = results.kind,
                label = results.label,
                options = results.visible,
                hasMore = results.hasMore,
                onLoadMore = finder::loadMore,
                onPick = { option ->
                    finder.dismissResults()
                    onPick(option)
                },
                onDismiss = finder::dismissResults,
            )
        }
    }
    if (state.scanning) {
        AppBottomSheet(onDismissRequest = finder::closeScanner, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.finder_scan_hint), style = MaterialTheme.typography.titleMedium)
                Box(Modifier.fillMaxWidth().aspectRatio(SCANNER_ASPECT)) {
                    BarcodeScannerPreview(
                        settings = scannerSettings,
                        active = true,
                        onBarcode = { code ->
                            scanFeedback(scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                            finder.onBarcode(code)
                        },
                        onPermissionDenied = finder::closeScanner,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private const val SCANNER_ASPECT = 4f / 3f
