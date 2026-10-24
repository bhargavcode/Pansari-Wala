package org.bhargav.pansariwala.feature.printer

import androidx.compose.runtime.Composable
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.printer.model.PrinterConnectionState
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printing.NothingToPrintException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.printer_error_bluetooth_off
import pansariwala.shared.generated.resources.printer_error_connection_failed
import pansariwala.shared.generated.resources.printer_error_connection_lost
import pansariwala.shared.generated.resources.printer_error_no_network
import pansariwala.shared.generated.resources.printer_error_not_connected
import pansariwala.shared.generated.resources.printer_error_not_found
import pansariwala.shared.generated.resources.printer_error_nothing_to_print
import pansariwala.shared.generated.resources.printer_error_permission
import pansariwala.shared.generated.resources.printer_error_timeout
import pansariwala.shared.generated.resources.printer_error_unsupported
import pansariwala.shared.generated.resources.printer_error_write_failed
import pansariwala.shared.generated.resources.printer_status_connected
import pansariwala.shared.generated.resources.printer_status_connecting
import pansariwala.shared.generated.resources.printer_status_disconnected
import pansariwala.shared.generated.resources.printer_type_ble
import pansariwala.shared.generated.resources.printer_type_bluetooth
import pansariwala.shared.generated.resources.printer_type_network
import pansariwala.shared.generated.resources.printer_type_usb

val PrinterError.messageRes: StringResource
    get() = when (this) {
        PrinterError.PERMISSION_DENIED -> Res.string.printer_error_permission
        PrinterError.BLUETOOTH_OFF -> Res.string.printer_error_bluetooth_off
        PrinterError.NO_NETWORK -> Res.string.printer_error_no_network
        PrinterError.UNSUPPORTED -> Res.string.printer_error_unsupported
        PrinterError.NOT_FOUND -> Res.string.printer_error_not_found
        PrinterError.CONNECTION_FAILED -> Res.string.printer_error_connection_failed
        PrinterError.CONNECTION_LOST -> Res.string.printer_error_connection_lost
        PrinterError.WRITE_FAILED -> Res.string.printer_error_write_failed
        PrinterError.NOT_CONNECTED -> Res.string.printer_error_not_connected
        PrinterError.TIMEOUT -> Res.string.printer_error_timeout
    }

val PrinterConnectionType.labelRes: StringResource
    get() = when (this) {
        PrinterConnectionType.BLUETOOTH -> Res.string.printer_type_bluetooth
        PrinterConnectionType.BLE -> Res.string.printer_type_ble
        PrinterConnectionType.NETWORK -> Res.string.printer_type_network
        PrinterConnectionType.USB -> Res.string.printer_type_usb
    }

@Composable
fun PrinterConnectionState.statusLabel(): String = stringResource(
    when (this) {
        is PrinterConnectionState.Connected -> Res.string.printer_status_connected
        is PrinterConnectionState.Connecting -> Res.string.printer_status_connecting
        else -> Res.string.printer_status_disconnected
    },
)

fun Throwable.toPrintMessage(): UiText = when (this) {
    is NothingToPrintException -> UiText.res(Res.string.printer_error_nothing_to_print)
    is PrinterException -> UiText.res(error.messageRes)
    else -> UiText.res(Res.string.printer_error_connection_failed)
}

suspend fun UiText.resolve(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> if (args.isEmpty()) getString(id) else getString(id, *args.toTypedArray())
}
