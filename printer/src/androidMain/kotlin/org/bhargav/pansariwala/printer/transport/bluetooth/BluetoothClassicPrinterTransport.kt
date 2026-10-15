package org.bhargav.pansariwala.printer.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.printer.AndroidPrinterPermissions
import org.bhargav.pansariwala.printer.registerSystemReceiver
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.transport.PrinterConnection
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.TransportAvailability
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Bluetooth Classic SPP printers (the common "58mm Bluetooth thermal printer"). */
@SuppressLint("MissingPermission")
class BluetoothClassicPrinterTransport(context: Context) : PrinterTransport {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter?
        get() = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override val type: PrinterConnectionType = PrinterConnectionType.BLUETOOTH

    override fun availability(): TransportAvailability {
        val adapter = adapter ?: return TransportAvailability.UNSUPPORTED
        if (!AndroidPrinterPermissions.hasBluetooth(appContext)) return TransportAvailability.UNAUTHORIZED
        return if (adapter.isEnabled) TransportAvailability.AVAILABLE else TransportAvailability.DISABLED
    }

    override fun scan(): Flow<PrinterDevice> = callbackFlow {
        val adapter = adapter ?: throw PrinterException(PrinterError.UNSUPPORTED)
        if (!adapter.isEnabled) throw PrinterException(PrinterError.BLUETOOTH_OFF)

        runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
            .filter { it.looksLikePrinter() }
            .forEach { trySend(it.toPrinterDevice(paired = true, rssi = null)) }

        // Ignore a FINISHED left over from cancelling someone else's discovery.
        var discoveryStarted = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> discoveryStarted = true
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.bluetoothDevice() ?: return
                        if (!device.looksLikePrinter()) return
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .takeIf { it != Short.MIN_VALUE }?.toInt()
                        trySend(device.toPrinterDevice(device.bondState == BluetoothDevice.BOND_BONDED, rssi))
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> if (discoveryStarted) channel.close()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        appContext.registerSystemReceiver(receiver, filter)
        val started = runCatching {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        }.getOrDefault(false)
        if (!started) channel.close()

        awaitClose {
            runCatching { adapter.cancelDiscovery() }
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    override suspend fun connect(device: PrinterDevice): PrinterConnection = withContext(Dispatchers.IO) {
        val adapter = adapter ?: throw PrinterException(PrinterError.UNSUPPORTED)
        if (!AndroidPrinterPermissions.hasBluetooth(appContext)) throw PrinterException(PrinterError.PERMISSION_DENIED)
        if (!adapter.isEnabled) throw PrinterException(PrinterError.BLUETOOTH_OFF)
        runCatching { adapter.cancelDiscovery() }
        val remote = runCatching { adapter.getRemoteDevice(device.address) }.getOrNull()
            ?: throw PrinterException(PrinterError.NOT_FOUND)

        val attempts: List<(BluetoothDevice) -> BluetoothSocket> = listOf(
            { it.createRfcommSocketToServiceRecord(SPP_UUID) },
            { it.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            // Some budget printers have no SDP record; channel 1 is the de-facto default.
            { it.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(it, 1) as BluetoothSocket },
        )
        var lastError: Throwable? = null
        for (create in attempts) {
            val socket = runCatching { create(remote) }.getOrNull() ?: continue
            try {
                connectCancellable(socket)
                return@withContext BluetoothClassicConnection(device, socket, appContext)
            } catch (e: CancellationException) {
                runCatching { socket.close() }
                throw e
            } catch (e: IOException) {
                lastError = e
                runCatching { socket.close() }
            }
        }
        throw PrinterException(PrinterError.CONNECTION_FAILED, lastError?.message, lastError)
    }

    /** [BluetoothSocket.connect] blocks for up to ~12 s; closing the socket is the only way to abort it. */
    private suspend fun connectCancellable(socket: BluetoothSocket) = coroutineScope {
        val connected = AtomicBoolean(false)
        val watchdog = launch {
            try {
                awaitCancellation()
            } finally {
                if (!connected.get()) runCatching { socket.close() }
            }
        }
        try {
            runInterruptible(Dispatchers.IO) { socket.connect() }
            connected.set(true)
        } finally {
            watchdog.cancel()
        }
    }

    private fun BluetoothDevice.looksLikePrinter(): Boolean {
        val major = runCatching { bluetoothClass?.majorDeviceClass }.getOrNull() ?: return true
        return major !in EXCLUDED_MAJOR_CLASSES
    }

    private fun BluetoothDevice.toPrinterDevice(paired: Boolean, rssi: Int?): PrinterDevice = PrinterDevice(
        id = address,
        name = runCatching { name }.getOrNull().orEmpty(),
        type = PrinterConnectionType.BLUETOOTH,
        address = address,
        isPaired = paired,
        signalStrength = rssi,
    )

    private fun Intent.bluetoothDevice(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val EXCLUDED_MAJOR_CLASSES = setOf(
            BluetoothClass.Device.Major.AUDIO_VIDEO,
            BluetoothClass.Device.Major.PHONE,
            BluetoothClass.Device.Major.COMPUTER,
            BluetoothClass.Device.Major.WEARABLE,
            BluetoothClass.Device.Major.HEALTH,
            BluetoothClass.Device.Major.TOY,
        )
    }
}

@SuppressLint("MissingPermission")
private class BluetoothClassicConnection(
    override val device: PrinterDevice,
    private val socket: BluetoothSocket,
    private val context: Context,
) : PrinterConnection {

    private val open = MutableStateFlow(true)
    override val isOpen: StateFlow<Boolean> = open.asStateFlow()

    /** Pushes link drops (printer off / out of range / BT disabled) without any polling. */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val address = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        }?.address
                    }.getOrNull()
                    if (address.equals(device.address, ignoreCase = true)) open.value = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) open.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerSystemReceiver(receiver, filter)
    }

    override suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        if (!open.value) throw PrinterException(PrinterError.NOT_CONNECTED)
        try {
            val out = socket.outputStream
            var offset = 0
            while (offset < data.size) {
                val len = minOf(CHUNK_SIZE, data.size - offset)
                out.write(data, offset, len)
                offset += len
            }
            out.flush()
        } catch (e: IOException) {
            open.value = false
            throw PrinterException(PrinterError.WRITE_FAILED, e.message, e)
        }
    }

    override suspend fun probe(statusRequest: ByteArray?): Boolean = open.value && socket.isConnected

    override suspend fun close() {
        open.value = false
        runCatching { context.unregisterReceiver(receiver) }
        withContext(Dispatchers.IO) { runCatching { socket.close() } }
    }

    private companion object {
        const val CHUNK_SIZE = 512
    }
}
