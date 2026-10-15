package org.bhargav.pansariwala.printer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.printer.document.PrintDocument
import org.bhargav.pansariwala.printer.encoder.EscPosEncoder
import org.bhargav.pansariwala.printer.encoder.PrinterCommandEncoder
import org.bhargav.pansariwala.printer.model.PrinterConnectionState
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.printer.transport.PrinterConnection
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.TransportAvailability

data class PrinterScanState(
    val isScanning: Boolean = false,
    val devices: List<PrinterDevice> = emptyList(),
    val scanningTypes: Set<PrinterConnectionType> = emptySet(),
    val error: PrinterError? = null,
)

data class PrinterManagerConfig(
    /** Scans stop on their own after this long to save battery. */
    val scanTimeoutMs: Long = 30_000,
    val connectTimeoutMs: Long = 15_000,
    /** Liveness probe interval while the app is in the foreground. */
    val monitorIntervalMs: Long = 15_000,
    val probeTimeoutMs: Long = 4_000,
    val reconnectInitialDelayMs: Long = 3_000,
    val reconnectMaxDelayMs: Long = 60_000,
    val settingsSaveDebounceMs: Long = 300,
)

/**
 * Single entry point for printing. Owns the active connection, scanning, settings and a light
 * foreground-only health monitor. All mutable state is confined to one serial dispatcher; blocking
 * I/O happens inside transports on their own dispatchers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrinterManager(
    transports: List<PrinterTransport>,
    private val store: PrinterSettingsStore = InMemoryPrinterSettingsStore(),
    private val encoder: PrinterCommandEncoder = EscPosEncoder(),
    private val config: PrinterManagerConfig = PrinterManagerConfig(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val transports: Map<PrinterConnectionType, PrinterTransport> = transports.associateBy { it.type }

    val supportedTypes: List<PrinterConnectionType> = transports.map { it.type }
    val isSupported: Boolean get() = supportedTypes.isNotEmpty()

    private val _connectionState = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _settings = MutableStateFlow(PrinterSettings())
    val settings: StateFlow<PrinterSettings> = _settings.asStateFlow()

    private val _scanState = MutableStateFlow(PrinterScanState())
    val scanState: StateFlow<PrinterScanState> = _scanState.asStateFlow()

    private val settingsLoaded = CompletableDeferred<Unit>()
    private val ioLock = Mutex()

    private var connection: PrinterConnection? = null
    private var watchJob: Job? = null
    private var scanJob: Job? = null
    private var connectJob: Job? = null
    private var monitorJob: Job? = null
    private var reconnectJob: Job? = null
    private var saveJob: Job? = null
    private var foreground = false
    private var userDisconnected = false

    init {
        scope.launch {
            runCatching { store.load() }.getOrNull()?.let { _settings.value = it }
            settingsLoaded.complete(Unit)
            if (foreground) resumeConnection()
        }
    }

    fun availability(type: PrinterConnectionType): TransportAvailability =
        transports[type]?.availability() ?: TransportAvailability.UNSUPPORTED

    // ---------------- lifecycle ----------------

    /** Call from the UI lifecycle (ON_START / ON_STOP). Monitoring and auto-reconnect only run in the foreground. */
    fun setAppInForeground(inForeground: Boolean) {
        scope.launch {
            if (foreground == inForeground) return@launch
            foreground = inForeground
            if (inForeground) {
                if (settingsLoaded.isCompleted) resumeConnection()
            } else {
                monitorJob?.cancel()
                reconnectJob?.cancel()
                scanJob?.cancel()
            }
        }
    }

    private fun resumeConnection() {
        val current = connection
        if (current != null) {
            if (current.isOpen.value) startMonitor() else handleConnectionLost(current)
            return
        }
        val last = _settings.value.lastDevice ?: return
        if (_settings.value.autoReconnect && !userDisconnected && connectJob?.isActive != true) {
            scheduleReconnect(last, initialDelayMs = 0)
        }
    }

    // ---------------- scanning ----------------

    fun startScan(types: Set<PrinterConnectionType> = supportedTypes.toSet()) {
        scope.launch {
            scanJob?.cancelAndJoin()
            val requested = types.mapNotNull { transports[it] }
            val usable = requested.filter { it.availability() == TransportAvailability.AVAILABLE }
            val blocked = (requested - usable.toSet()).firstOrNull()?.let { it.availability().toError(it.type) }
            val scannedTypes = usable.map { it.type }.toSet()
            val kept = _scanState.value.devices.filter { it.type !in scannedTypes }
            val seed = (kept + listOfNotNull(_settings.value.lastDevice?.takeIf { it.type in scannedTypes }))
                .distinctBy { it.id }
            _scanState.value = PrinterScanState(
                isScanning = usable.isNotEmpty(),
                devices = seed,
                scanningTypes = scannedTypes,
                error = blocked,
            )
            if (usable.isEmpty()) return@launch
            scanJob = scope.launch {
                try {
                    withTimeoutOrNull(config.scanTimeoutMs) {
                        usable.map { transport ->
                            transport.scan().catch { e ->
                                val error = (e as? PrinterException)?.error ?: PrinterError.UNSUPPORTED
                                _scanState.update { it.copy(error = error) }
                            }
                        }.merge().collect(::upsertDevice)
                    }
                } finally {
                    _scanState.update { it.copy(isScanning = false, scanningTypes = emptySet()) }
                }
            }
        }
    }

    fun stopScan() {
        scope.launch { scanJob?.cancel() }
    }

    /** Adds a device that cannot be discovered (e.g. a network printer typed in by IP). */
    fun addManualDevice(device: PrinterDevice) {
        scope.launch { upsertDevice(device) }
    }

    private fun upsertDevice(device: PrinterDevice) {
        _scanState.update { state ->
            val index = state.devices.indexOfFirst { it.id == device.id }
            val devices = if (index >= 0) {
                state.devices.toMutableList().also { list ->
                    val old = list[index]
                    list[index] = device.copy(
                        name = device.name.ifBlank { old.name },
                        isPaired = device.isPaired || old.isPaired,
                    )
                }
            } else {
                state.devices + device
            }
            state.copy(devices = devices.sortedWith(compareByDescending<PrinterDevice> { it.isPaired }.thenBy { it.displayName.lowercase() }))
        }
    }

    // ---------------- connection ----------------

    fun connect(device: PrinterDevice) {
        scope.launch {
            scanJob?.cancelAndJoin()
            reconnectJob?.cancel()
            connectJob?.cancelAndJoin()
            userDisconnected = false
            connectJob = scope.launch { openConnection(device, userInitiated = true) }
        }
    }

    /** Disconnects; with [forget] the printer is also removed from auto-reconnect. */
    fun disconnect(forget: Boolean = false) {
        scope.launch {
            userDisconnected = true
            reconnectJob?.cancel()
            connectJob?.cancelAndJoin()
            monitorJob?.cancel()
            ioLock.withLock { closeCurrent() }
            _connectionState.value = PrinterConnectionState.Disconnected
            if (forget) updateSettings { it.copy(lastDevice = null) }
        }
    }

    private suspend fun openConnection(device: PrinterDevice, userInitiated: Boolean): Boolean {
        val transport = transports[device.type]
        if (transport == null) {
            _connectionState.value = PrinterConnectionState.Failed(device, PrinterError.UNSUPPORTED)
            return false
        }
        return ioLock.withLock {
            closeCurrent()
            _connectionState.value = PrinterConnectionState.Connecting(device)
            try {
                val opened = withTimeout(config.connectTimeoutMs) { transport.connect(device) }
                connection = opened
                _connectionState.value = PrinterConnectionState.Connected(device)
                if (_settings.value.lastDevice != device) updateSettings { it.copy(lastDevice = device) }
                watch(opened)
                startMonitor()
                true
            } catch (e: TimeoutCancellationException) {
                fail(device, PrinterError.TIMEOUT, userInitiated)
            } catch (e: CancellationException) {
                _connectionState.value = PrinterConnectionState.Disconnected
                throw e
            } catch (e: Throwable) {
                fail(device, (e as? PrinterException)?.error ?: PrinterError.CONNECTION_FAILED, userInitiated)
            }
        }
    }

    private fun fail(device: PrinterDevice, error: PrinterError, userInitiated: Boolean): Boolean {
        _connectionState.value = if (userInitiated) {
            PrinterConnectionState.Failed(device, error)
        } else {
            PrinterConnectionState.Disconnected
        }
        return false
    }

    private fun watch(opened: PrinterConnection) {
        watchJob?.cancel()
        watchJob = scope.launch {
            opened.isOpen.first { !it }
            handleConnectionLost(opened)
        }
    }

    private fun handleConnectionLost(lost: PrinterConnection) {
        if (connection !== lost) return
        connection = null
        monitorJob?.cancel()
        scope.launch { runCatching { lost.close() } }
        _connectionState.value = PrinterConnectionState.Failed(lost.device, PrinterError.CONNECTION_LOST)
        if (foreground && _settings.value.autoReconnect && !userDisconnected) {
            scheduleReconnect(lost.device, config.reconnectInitialDelayMs)
        }
    }

    private fun scheduleReconnect(device: PrinterDevice, initialDelayMs: Long) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var wait = initialDelayMs
            while (foreground && connection == null && !userDisconnected) {
                if (wait > 0) delay(wait)
                if (transports[device.type]?.availability() == TransportAvailability.AVAILABLE &&
                    openConnection(device, userInitiated = false)
                ) {
                    break
                }
                wait = (wait.coerceAtLeast(config.reconnectInitialDelayMs) * 2).coerceAtMost(config.reconnectMaxDelayMs)
            }
        }
    }

    private fun startMonitor() {
        if (!foreground) return
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                delay(config.monitorIntervalMs)
                val current = connection ?: break
                val alive = ioLock.withLock {
                    try {
                        withTimeoutOrNull(config.probeTimeoutMs) { current.probe(encoder.statusRequest()) } ?: true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        false
                    }
                }
                if (!alive) {
                    handleConnectionLost(current)
                    break
                }
            }
        }
    }

    private suspend fun closeCurrent() {
        watchJob?.cancel()
        val current = connection ?: return
        connection = null
        runCatching { current.close() }
    }

    // ---------------- printing ----------------

    /**
     * Encodes and sends [document]. Reconnects to the last printer if needed. Runs to completion even
     * if the caller is cancelled so a receipt is never half printed.
     */
    suspend fun print(document: PrintDocument): Result<Unit> {
        settingsLoaded.await()
        val bytes = withContext(Dispatchers.Default) { encoder.encode(document, _settings.value) }
        return printRaw(bytes)
    }

    suspend fun printRaw(bytes: ByteArray): Result<Unit> = scope.async {
        connectJob?.takeIf { it.isActive }?.join()
        var current = connection?.takeIf { it.isOpen.value }
        if (current == null) {
            val last = _settings.value.lastDevice
                ?: return@async Result.failure(PrinterException(PrinterError.NOT_CONNECTED))
            reconnectJob?.cancel()
            userDisconnected = false
            if (!openConnection(last, userInitiated = true)) {
                val error = (_connectionState.value as? PrinterConnectionState.Failed)?.reason ?: PrinterError.NOT_CONNECTED
                return@async Result.failure(PrinterException(error))
            }
            current = connection ?: return@async Result.failure(PrinterException(PrinterError.NOT_CONNECTED))
        }
        try {
            ioLock.withLock { current.write(bytes) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            handleConnectionLost(current)
            Result.failure(e as? PrinterException ?: PrinterException(PrinterError.WRITE_FAILED, cause = e))
        }
    }.await()

    // ---------------- settings ----------------

    fun updateSettings(transform: (PrinterSettings) -> PrinterSettings) {
        _settings.update(transform)
        scope.launch {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(config.settingsSaveDebounceMs)
                settingsLoaded.await()
                runCatching { store.save(_settings.value) }
            }
        }
    }

    /** Releases sockets and stops background work. The manager cannot be reused afterwards. */
    fun shutdown() {
        scope.launch {
            ioLock.withLock { closeCurrent() }
            scope.cancel()
        }
    }

    private fun TransportAvailability.toError(type: PrinterConnectionType): PrinterError? = when (this) {
        TransportAvailability.AVAILABLE -> null
        TransportAvailability.DISABLED ->
            if (type == PrinterConnectionType.NETWORK) PrinterError.NO_NETWORK else PrinterError.BLUETOOTH_OFF
        TransportAvailability.UNAUTHORIZED -> PrinterError.PERMISSION_DENIED
        TransportAvailability.UNSUPPORTED -> PrinterError.UNSUPPORTED
    }
}

fun networkPrinterDevice(host: String, port: Int = DEFAULT_RAW_PRINT_PORT, name: String = host): PrinterDevice =
    PrinterDevice(
        id = "$host:$port",
        name = name,
        type = PrinterConnectionType.NETWORK,
        address = host,
        port = port,
    )

const val DEFAULT_RAW_PRINT_PORT: Int = 9100
