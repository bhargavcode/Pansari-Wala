package org.bhargav.pansariwala.feature.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.pickImageCatching
import org.bhargav.pansariwala.printer.DEFAULT_RAW_PRINT_PORT
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.printer.PrinterScanState
import org.bhargav.pansariwala.printer.image.ImageProcessor
import org.bhargav.pansariwala.printer.model.ArgbImage
import org.bhargav.pansariwala.printer.model.PrintImageSettings
import org.bhargav.pansariwala.printer.model.PrinterConnectionState
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.printer.networkPrinterDevice
import org.bhargav.pansariwala.printer.transport.TransportAvailability
import org.bhargav.pansariwala.printing.PosPrintService
import org.bhargav.pansariwala.printing.decodePrintImage
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.printer_logo_failed
import pansariwala.shared.generated.resources.printer_manual_invalid
import pansariwala.shared.generated.resources.printer_print_success

enum class PrintJob { TEST_PAGE, RECEIPT, TICKET, INVOICE, LABELS, INVENTORY, LOW_STOCK }

data class PrinterSettingsUiState(
    val connection: PrinterConnectionState = PrinterConnectionState.Disconnected,
    val scan: PrinterScanState = PrinterScanState(),
    val settings: PrinterSettings = PrinterSettings(),
    val supportedTypes: List<PrinterConnectionType> = emptyList(),
    val selectedTypes: Set<PrinterConnectionType> = emptySet(),
    val runningJob: PrintJob? = null,
    val hasLogoSource: Boolean = false,
    val isProcessingLogo: Boolean = false,
) {
    val isSupported: Boolean get() = supportedTypes.isNotEmpty()
}

private data class LocalState(
    val selectedTypes: Set<PrinterConnectionType>,
    val runningJob: PrintJob?,
    val hasLogoSource: Boolean,
    val isProcessingLogo: Boolean,
)

class PrinterSettingsViewModel(
    private val printer: PrinterManager,
    private val printService: PosPrintService,
    private val imagePicker: ImagePicker,
) : ViewModel() {

    private val selectedTypes = MutableStateFlow(printer.supportedTypes.toSet())
    private val runningJob = MutableStateFlow<PrintJob?>(null)
    private val logoSource = MutableStateFlow<ArgbImage?>(null)
    private val processingLogo = MutableStateFlow(false)
    private var reprocessJob: Job? = null

    private val messageChannel = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = messageChannel.receiveAsFlow()

    private val localState = combine(selectedTypes, runningJob, logoSource, processingLogo) { types, job, source, processing ->
        LocalState(types, job, source != null, processing)
    }

    val uiState: StateFlow<PrinterSettingsUiState> = combine(
        printer.connectionState,
        printer.scanState,
        printer.settings,
        localState,
    ) { connection, scan, settings, local ->
        PrinterSettingsUiState(
            connection = connection,
            scan = scan,
            settings = settings,
            supportedTypes = printer.supportedTypes,
            selectedTypes = local.selectedTypes,
            runningJob = local.runningJob,
            hasLogoSource = local.hasLogoSource,
            isProcessingLogo = local.isProcessingLogo,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        PrinterSettingsUiState(supportedTypes = printer.supportedTypes, selectedTypes = selectedTypes.value),
    )

    // ---------- discovery & connection ----------

    fun toggleType(type: PrinterConnectionType) {
        val next = selectedTypes.value.let { if (type in it) it - type else it + type }
        if (next.isNotEmpty()) selectedTypes.value = next
    }

    fun startScan() = printer.startScan(selectedTypes.value)

    fun needsBluetoothPermission(): Boolean = bluetoothAvailability().any { it == TransportAvailability.UNAUTHORIZED }

    fun bluetoothDisabled(): Boolean = bluetoothAvailability().any { it == TransportAvailability.DISABLED }

    private fun bluetoothAvailability(): List<TransportAvailability> =
        selectedTypes.value.filter { it != PrinterConnectionType.NETWORK }.map(printer::availability)

    fun stopScan() = printer.stopScan()

    fun connect(device: PrinterDevice) = printer.connect(device)

    fun disconnect() = printer.disconnect()

    fun forget() = printer.disconnect(forget = true)

    fun reconnect() {
        printer.settings.value.lastDevice?.let(printer::connect)
    }

    /** Returns false (and shows a message) when the address is not a valid IPv4 / port. */
    fun addNetworkPrinter(host: String, portText: String): Boolean {
        val ip = host.trim()
        val port = portText.trim().ifBlank { DEFAULT_RAW_PRINT_PORT.toString() }.toIntOrNull()
        val octets = ip.split('.').mapNotNull { it.toIntOrNull() }
        val valid = octets.size == 4 && octets.all { it in 0..255 } && port != null && port in 1..65535
        if (!valid) {
            messageChannel.trySend(UiText.res(Res.string.printer_manual_invalid))
            return false
        }
        val device = networkPrinterDevice(ip, port)
        printer.addManualDevice(device)
        printer.connect(device)
        return true
    }

    // ---------- settings ----------

    fun update(transform: (PrinterSettings) -> PrinterSettings) {
        val before = printer.settings.value.paperWidth
        printer.updateSettings(transform)
        if (printer.settings.value.paperWidth != before) scheduleLogoReprocess()
    }

    fun updateImage(transform: (PrintImageSettings) -> PrintImageSettings) {
        printer.updateSettings { it.copy(image = transform(it.image)) }
        scheduleLogoReprocess()
    }

    fun pickLogo() {
        viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(ImageSource.GALLERY).getOrElse {
                messageChannel.send(UiText.res(Res.string.printer_logo_failed))
                return@launch
            } ?: return@launch
            processingLogo.value = true
            val decoded = decodePrintImage(picked.bytes, printer.settings.value.paperWidth.dots)
            if (decoded == null) {
                processingLogo.value = false
                messageChannel.send(UiText.res(Res.string.printer_logo_failed))
                return@launch
            }
            logoSource.value = decoded
            printer.updateSettings { it.copy(image = it.image.copy(printLogo = true)) }
            reprocessLogo()
        }
    }

    fun removeLogo() {
        logoSource.value = null
        printer.updateSettings { it.copy(image = it.image.copy(logo = null, printLogo = false)) }
    }

    private fun scheduleLogoReprocess() {
        if (logoSource.value == null) return
        reprocessJob?.cancel()
        reprocessJob = viewModelScope.launch {
            delay(LOGO_REPROCESS_DEBOUNCE_MS)
            reprocessLogo()
        }
    }

    private suspend fun reprocessLogo() {
        val source = logoSource.value ?: return
        processingLogo.value = true
        val settings = printer.settings.value
        val image = settings.image
        val width = settings.paperWidth.dots * image.logoWidthPercent.coerceIn(MIN_LOGO_PERCENT, 100) / 100
        val mono = withContext(Dispatchers.Default) {
            ImageProcessor.toMonochrome(
                image = source,
                targetWidth = width,
                mode = image.ditherMode,
                threshold = image.threshold,
                brightness = image.brightness,
                invert = image.invert,
                contrast = image.contrast,
                gammaPercent = image.gammaPercent,
            )
        }
        printer.updateSettings { it.copy(image = it.image.copy(logo = mono)) }
        processingLogo.value = false
    }

    // ---------- printing ----------

    fun print(job: PrintJob) {
        if (runningJob.value != null) return
        viewModelScope.launch {
            runningJob.value = job
            val result = when (job) {
                PrintJob.TEST_PAGE -> printService.printTestPage()
                PrintJob.RECEIPT -> printService.printReceipt()
                PrintJob.TICKET -> printService.printOrderTicket()
                PrintJob.INVOICE -> printService.printInvoice()
                PrintJob.LABELS -> printService.printLabels()
                PrintJob.INVENTORY -> printService.printInventory(lowStockOnly = false)
                PrintJob.LOW_STOCK -> printService.printInventory(lowStockOnly = true)
            }
            runningJob.value = null
            messageChannel.send(
                result.fold(
                    onSuccess = { UiText.res(Res.string.printer_print_success) },
                    onFailure = { it.toPrintMessage() },
                ),
            )
        }
    }

    override fun onCleared() {
        printer.stopScan()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val LOGO_REPROCESS_DEBOUNCE_MS = 150L
        const val MIN_LOGO_PERCENT = 10
    }
}
