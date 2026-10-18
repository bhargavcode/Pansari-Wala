package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.sync.OrderSyncSignal
import org.bhargav.pansariwala.feature.catalog.SheetImportUiState
import org.bhargav.pansariwala.platform.DocumentPicker
import org.bhargav.pansariwala.platform.PickedDocument
import org.bhargav.pansariwala.util.AppConstants

/** POS inventory Excel import: pick → server dry run → apply → pull the updated catalog. */
class ShopSheetImportViewModel(
    private val api: PansariApi,
    private val documentPicker: DocumentPicker,
    private val syncSignal: OrderSyncSignal,
) : ViewModel() {

    private val _state = MutableStateFlow<SheetImportUiState?>(null)
    val state: StateFlow<SheetImportUiState?> = _state.asStateFlow()

    /** Opened by the UI with LocalUriHandler. */
    private val _openUrl = MutableStateFlow<String?>(null)
    val openUrl: StateFlow<String?> = _openUrl.asStateFlow()

    private var picked: PickedDocument? = null

    fun open() {
        picked = null
        _state.value = SheetImportUiState()
    }

    fun dismiss() {
        picked = null
        _state.value = null
    }

    fun urlOpened() {
        _openUrl.value = null
    }

    fun downloadSample() {
        _openUrl.value = api.productTemplateUrl
    }

    fun export() = runBusy { _openUrl.value = api.shopProductsExportUrl() }

    fun pickFile() {
        viewModelScope.launch {
            val doc = documentPicker.pickDocument(
                AppConstants.ProductSheet.PICKER_MIME_TYPES,
                AppConstants.ProductSheet.PICKER_EXTENSIONS,
            ) ?: return@launch
            picked = doc
            upload(doc, dryRun = true)
        }
    }

    fun apply() {
        val doc = picked ?: return
        viewModelScope.launch {
            upload(doc, dryRun = false)
            if (_state.value?.report?.dryRun == false) syncSignal.kick(pull = true)
        }
    }

    private suspend fun upload(doc: PickedDocument, dryRun: Boolean) {
        _state.update { it?.copy(busy = true, fileName = doc.name, error = null) }
        try {
            val report = api.importShopProducts(doc.name, doc.bytes, dryRun)
            _state.update { it?.copy(busy = false, report = report) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.update { it?.copy(busy = false, error = error.toApiUiText()) }
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it?.copy(busy = true, error = null) }
            try {
                block()
                _state.update { it?.copy(busy = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it?.copy(busy = false, error = error.toApiUiText()) }
            }
        }
    }
}
