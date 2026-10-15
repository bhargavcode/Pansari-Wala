package org.bhargav.pansariwala.printer.transport.bluetooth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.transport.PrinterConnection
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.TransportAvailability
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBCharacteristicPropertyWriteWithoutResponse
import platform.CoreBluetooth.CBCharacteristicWriteType
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBCharacteristicWriteWithoutResponse
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationDenied
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateResetting
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnknown
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.darwin.NSObject

/**
 * BLE printers via CoreBluetooth (iOS cannot use Bluetooth Classic SPP without MFi).
 * Every CoreBluetooth call runs on the main queue, which is also where the delegates fire.
 */
@OptIn(ExperimentalForeignApi::class)
class BlePrinterTransport : PrinterTransport {

    override val type: PrinterConnectionType = PrinterConnectionType.BLE

    private val centralDelegate = CentralDelegate()
    private var centralOrNull: CBCentralManager? = null
    private val peripherals = mutableMapOf<String, CBPeripheral>()

    /** Created lazily: instantiating the manager is what triggers the iOS Bluetooth permission prompt. */
    private val central: CBCentralManager
        get() = centralOrNull ?: CBCentralManager(
            delegate = centralDelegate,
            queue = null,
            options = mapOf<Any?, Any?>(CBCentralManagerOptionShowPowerAlertKey to true),
        ).also { centralOrNull = it }

    override fun availability(): TransportAvailability {
        val auth = CBManager.authorization
        if (auth == CBManagerAuthorizationDenied || auth == CBManagerAuthorizationRestricted) {
            return TransportAvailability.UNAUTHORIZED
        }
        return when (centralDelegate.state.value) {
            CBManagerStatePoweredOff -> TransportAvailability.DISABLED
            CBManagerStateUnsupported -> TransportAvailability.UNSUPPORTED
            CBManagerStateUnauthorized -> TransportAvailability.UNAUTHORIZED
            else -> TransportAvailability.AVAILABLE
        }
    }

    override fun scan(): Flow<PrinterDevice> = callbackFlow {
        val manager = central
        awaitPoweredOn()
        centralDelegate.onDiscover = { peripheral, name, rssi ->
            val id = peripheral.identifier.UUIDString
            peripherals[id] = peripheral
            trySend(
                PrinterDevice(
                    id = id,
                    name = name,
                    type = PrinterConnectionType.BLE,
                    address = id,
                    signalStrength = rssi,
                ),
            )
        }
        manager.retrieveConnectedPeripheralsWithServices(KNOWN_SERVICE_UUIDS.map { CBUUID.UUIDWithString(it) })
            .filterIsInstance<CBPeripheral>()
            .forEach { peripheral ->
                val id = peripheral.identifier.UUIDString
                peripherals[id] = peripheral
                trySend(PrinterDevice(id, peripheral.name.orEmpty(), PrinterConnectionType.BLE, id, isPaired = true))
            }
        manager.scanForPeripheralsWithServices(
            serviceUUIDs = null,
            options = mapOf<Any?, Any?>(CBCentralManagerScanOptionAllowDuplicatesKey to false),
        )
        awaitClose {
            centralDelegate.onDiscover = null
            runCatching { manager.stopScan() }
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun connect(device: PrinterDevice): PrinterConnection = withContext(Dispatchers.Main) {
        val manager = central
        awaitPoweredOn()
        manager.stopScan()
        val peripheral = peripherals[device.id]
            ?: NSUUID(uUIDString = device.id).let { uuid ->
                manager.retrievePeripheralsWithIdentifiers(listOf(uuid)).firstOrNull() as? CBPeripheral
            }
            ?: throw PrinterException(PrinterError.NOT_FOUND)
        peripherals[device.id] = peripheral
        BleConnection(device, peripheral, manager, centralDelegate).also { it.open() }
    }

    private suspend fun awaitPoweredOn() {
        val state = withTimeoutOrNull(STATE_TIMEOUT_MS) {
            centralDelegate.state.first { it != CBManagerStateUnknown && it != CBManagerStateResetting }
        } ?: CBManagerStateUnknown
        when (state) {
            CBManagerStatePoweredOn -> Unit
            CBManagerStatePoweredOff -> throw PrinterException(PrinterError.BLUETOOTH_OFF)
            CBManagerStateUnauthorized -> throw PrinterException(PrinterError.PERMISSION_DENIED)
            CBManagerStateUnsupported -> throw PrinterException(PrinterError.UNSUPPORTED)
            else -> throw PrinterException(PrinterError.TIMEOUT)
        }
    }

    internal companion object {
        const val STATE_TIMEOUT_MS = 5_000L

        /** Common write services on BLE thermal printers (ISSC, Goojprt/MTP, 18F0, FF00). */
        val KNOWN_SERVICE_UUIDS = listOf(
            "49535343-FE7D-4AE5-8FA9-9FAFD205E455",
            "E7810A71-73AE-499D-8C15-FAA9AEF0C3F2",
            "18F0",
            "FF00",
        )
        val PREFERRED_CHARACTERISTIC_UUIDS = setOf(
            "49535343-8841-43F4-A8D4-ECBE34729BB3",
            "BEF8D6C9-9C21-4C9E-B632-BD58C1009F9F",
            "2AF1",
            "FF02",
        )
    }
}

internal interface PeripheralLinkListener {
    fun onConnected()
    fun onFailed(error: NSError?)
    fun onDisconnected(error: NSError?)
}

@OptIn(ExperimentalForeignApi::class)
internal class CentralDelegate : NSObject(), CBCentralManagerDelegateProtocol {
    val state = MutableStateFlow<CBManagerState>(CBManagerStateUnknown)
    var onDiscover: ((CBPeripheral, String, Int?) -> Unit)? = null
    val listeners = mutableMapOf<String, PeripheralLinkListener>()

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        state.value = central.state
        if (central.state != CBManagerStatePoweredOn) {
            listeners.values.toList().forEach { it.onDisconnected(null) }
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber,
    ) {
        val name = didDiscoverPeripheral.name
            ?: advertisementData[CBAdvertisementDataLocalNameKey] as? String
        if (name.isNullOrBlank()) return
        onDiscover?.invoke(didDiscoverPeripheral, name, RSSI.intValue.takeIf { it != 127 })
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        listeners[didConnectPeripheral.identifier.UUIDString]?.onConnected()
    }

    @ObjCSignatureOverride
    override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
        listeners[didFailToConnectPeripheral.identifier.UUIDString]?.onFailed(error)
    }

    @ObjCSignatureOverride
    override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?) {
        listeners[didDisconnectPeripheral.identifier.UUIDString]?.onDisconnected(error)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class PeripheralDelegate : NSObject(), CBPeripheralDelegateProtocol {
    var onServices: ((NSError?) -> Unit)? = null
    var onCharacteristics: ((CBService, NSError?) -> Unit)? = null
    var onWrite: ((NSError?) -> Unit)? = null
    var onReadyToWrite: (() -> Unit)? = null

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        onServices?.invoke(didDiscoverServices)
    }

    @ObjCSignatureOverride
    override fun peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService: CBService, error: NSError?) {
        onCharacteristics?.invoke(didDiscoverCharacteristicsForService, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(peripheral: CBPeripheral, didWriteValueForCharacteristic: CBCharacteristic, error: NSError?) {
        onWrite?.invoke(error)
    }

    override fun peripheralIsReadyToSendWriteWithoutResponse(peripheral: CBPeripheral) {
        onReadyToWrite?.invoke()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class BleConnection(
    override val device: PrinterDevice,
    private val peripheral: CBPeripheral,
    private val central: CBCentralManager,
    private val centralDelegate: CentralDelegate,
) : PrinterConnection {

    private val open = MutableStateFlow(false)
    override val isOpen: StateFlow<Boolean> = open.asStateFlow()
    private val peripheralDelegate = PeripheralDelegate()
    private var characteristic: CBCharacteristic? = null
    private val id = peripheral.identifier.UUIDString

    suspend fun open() {
        val linked = CompletableDeferred<Unit>()
        centralDelegate.listeners[id] = object : PeripheralLinkListener {
            override fun onConnected() {
                linked.complete(Unit)
            }

            override fun onFailed(error: NSError?) {
                linked.completeExceptionally(PrinterException(PrinterError.CONNECTION_FAILED, error?.localizedDescription))
            }

            override fun onDisconnected(error: NSError?) {
                open.value = false
                linked.completeExceptionally(PrinterException(PrinterError.CONNECTION_LOST, error?.localizedDescription))
            }
        }
        peripheral.delegate = peripheralDelegate
        try {
            central.connectPeripheral(peripheral, options = null)
            linked.await()
            discoverServices()
            characteristic = discoverWritableCharacteristic()
                ?: throw PrinterException(PrinterError.UNSUPPORTED)
            open.value = true
        } catch (e: Throwable) {
            release()
            if (e is CancellationException || e is PrinterException) throw e
            throw PrinterException(PrinterError.CONNECTION_FAILED, e.message, e)
        }
    }

    private suspend fun discoverServices() {
        val done = CompletableDeferred<Unit>()
        peripheralDelegate.onServices = { error ->
            if (error == null) done.complete(Unit)
            else done.completeExceptionally(PrinterException(PrinterError.CONNECTION_FAILED, error.localizedDescription))
        }
        peripheral.discoverServices(null)
        done.await()
        peripheralDelegate.onServices = null
    }

    private suspend fun discoverWritableCharacteristic(): CBCharacteristic? {
        val services = peripheral.services.orEmpty().filterIsInstance<CBService>()
        if (services.isEmpty()) return null
        val pending = services.map { it.UUID.UUIDString }.toMutableSet()
        val done = CompletableDeferred<Unit>()
        peripheralDelegate.onCharacteristics = { service, _ ->
            pending -= service.UUID.UUIDString
            if (pending.isEmpty()) done.complete(Unit)
        }
        services.forEach { peripheral.discoverCharacteristics(null, forService = it) }
        withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { done.await() }
        peripheralDelegate.onCharacteristics = null

        val writable = services
            .flatMap { it.characteristics.orEmpty().filterIsInstance<CBCharacteristic>() }
            .filter { it.properties and (CBCharacteristicPropertyWrite or CBCharacteristicPropertyWriteWithoutResponse) != 0uL }
        return writable.firstOrNull { it.UUID.UUIDString.uppercase() in BlePrinterTransport.PREFERRED_CHARACTERISTIC_UUIDS }
            ?: writable.firstOrNull()
    }

    override suspend fun write(data: ByteArray) = withContext(Dispatchers.Main) {
        val target = characteristic
        if (!open.value || target == null) throw PrinterException(PrinterError.NOT_CONNECTED)
        val withoutResponse = target.properties and CBCharacteristicPropertyWriteWithoutResponse != 0uL
        val writeType: CBCharacteristicWriteType =
            if (withoutResponse) CBCharacteristicWriteWithoutResponse else CBCharacteristicWriteWithResponse
        val chunk = peripheral.maximumWriteValueLengthForType(writeType).toInt().coerceIn(MIN_CHUNK, MAX_CHUNK)
        var offset = 0
        while (offset < data.size) {
            if (!open.value) throw PrinterException(PrinterError.CONNECTION_LOST)
            val len = minOf(chunk, data.size - offset)
            val payload = data.toNSData(offset, len)
            if (withoutResponse) {
                awaitWriteWindow()
                peripheral.writeValue(payload, forCharacteristic = target, type = writeType)
                delay(CHUNK_PACING_MS)
            } else {
                val ack = CompletableDeferred<Unit>()
                peripheralDelegate.onWrite = { error ->
                    if (error == null) ack.complete(Unit)
                    else ack.completeExceptionally(PrinterException(PrinterError.WRITE_FAILED, error.localizedDescription))
                }
                peripheral.writeValue(payload, forCharacteristic = target, type = writeType)
                withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) { ack.await() }
                    ?: throw PrinterException(PrinterError.TIMEOUT)
            }
            offset += len
        }
        peripheralDelegate.onWrite = null
    }

    private suspend fun awaitWriteWindow() {
        if (peripheral.canSendWriteWithoutResponse) return
        val ready = CompletableDeferred<Unit>()
        peripheralDelegate.onReadyToWrite = { ready.complete(Unit) }
        withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) { ready.await() }
        peripheralDelegate.onReadyToWrite = null
    }

    override suspend fun probe(statusRequest: ByteArray?): Boolean = open.value

    override suspend fun close() = withContext(Dispatchers.Main) { release() }

    private fun release() {
        open.value = false
        centralDelegate.listeners.remove(id)
        peripheral.delegate = null
        runCatching { central.cancelPeripheralConnection(peripheral) }
    }

    private fun ByteArray.toNSData(offset: Int, length: Int): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(offset), length = length.convert())
    }

    private companion object {
        const val MIN_CHUNK = 20
        const val MAX_CHUNK = 512
        const val CHUNK_PACING_MS = 4L
        const val WRITE_ACK_TIMEOUT_MS = 3_000L
        const val DISCOVERY_TIMEOUT_MS = 5_000L
    }
}
