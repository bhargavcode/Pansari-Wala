package org.bhargav.pansariwala.printer.model

import kotlinx.serialization.Serializable

/** How the app talks to a printer. Add a new entry + a [org.bhargav.pansariwala.printer.transport.PrinterTransport] to support more. */
@Serializable
enum class PrinterConnectionType {
    /** Bluetooth Classic (SPP / RFCOMM). Android only. */
    BLUETOOTH,

    /** Bluetooth Low Energy (GATT write characteristic). iOS. */
    BLE,

    /** Raw TCP socket, usually port 9100 (Wi-Fi / Ethernet printers). */
    NETWORK,

    /** USB cable (WebUSB / WebSerial in browsers). */
    USB,
}

@Serializable
data class PrinterDevice(
    /** Stable identifier: MAC on Android, peripheral UUID on iOS, `host:port` for network. */
    val id: String,
    val name: String,
    val type: PrinterConnectionType,
    val address: String,
    val port: Int = 0,
    /** True when the OS already knows this device (paired / bonded). */
    val isPaired: Boolean = false,
    val signalStrength: Int? = null,
) {
    val displayName: String get() = name.ifBlank { address }
}

sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data class Connecting(val device: PrinterDevice) : PrinterConnectionState
    data class Connected(val device: PrinterDevice) : PrinterConnectionState
    data class Failed(val device: PrinterDevice?, val reason: PrinterError) : PrinterConnectionState

    val deviceOrNull: PrinterDevice?
        get() = when (this) {
            Disconnected -> null
            is Connecting -> device
            is Connected -> device
            is Failed -> device
        }
}

/** Machine-readable failure reasons; UI maps these to localized strings. */
enum class PrinterError {
    PERMISSION_DENIED,
    BLUETOOTH_OFF,
    NO_NETWORK,
    UNSUPPORTED,
    NOT_FOUND,
    CONNECTION_FAILED,
    CONNECTION_LOST,
    WRITE_FAILED,
    NOT_CONNECTED,
    TIMEOUT,
}

class PrinterException(val error: PrinterError, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: error.name, cause)
