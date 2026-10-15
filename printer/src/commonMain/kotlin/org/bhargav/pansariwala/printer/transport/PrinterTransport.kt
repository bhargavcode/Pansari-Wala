package org.bhargav.pansariwala.printer.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice

enum class TransportAvailability {
    AVAILABLE,

    /** Radio is off (e.g. Bluetooth disabled). */
    DISABLED,

    /** Runtime permission missing. */
    UNAUTHORIZED,
    UNSUPPORTED,
}

/**
 * One way of reaching printers (Bluetooth Classic, BLE, TCP, USB, cloud…).
 * Add a new implementation and register it with [org.bhargav.pansariwala.printer.PrinterManager] to support another printer kind.
 */
interface PrinterTransport {
    val type: PrinterConnectionType

    fun availability(): TransportAvailability

    /**
     * Cold flow of devices found while collected. Cancelling the collector must stop any radio scan,
     * which is what makes scanning lifecycle-aware for callers.
     */
    fun scan(): Flow<PrinterDevice>

    /** Opens a connection; throws [org.bhargav.pansariwala.printer.model.PrinterException] on failure. */
    suspend fun connect(device: PrinterDevice): PrinterConnection
}

interface PrinterConnection {
    val device: PrinterDevice

    /** Flips to false as soon as the transport notices the link dropped. */
    val isOpen: StateFlow<Boolean>

    suspend fun write(data: ByteArray)

    /**
     * Cheap liveness check used by the background monitor. [statusRequest] is a printer-language
     * status command the transport may send; return false only when the link is definitely gone.
     */
    suspend fun probe(statusRequest: ByteArray?): Boolean = isOpen.value

    suspend fun close()
}
