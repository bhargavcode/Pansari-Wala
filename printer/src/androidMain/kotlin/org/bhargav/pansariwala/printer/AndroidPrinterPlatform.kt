package org.bhargav.pansariwala.printer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.bluetooth.BluetoothClassicPrinterTransport
import org.bhargav.pansariwala.printer.transport.network.NetworkPrinterTransport

object AndroidPrinterTransports {
    fun create(context: Context): List<PrinterTransport> = listOf(
        BluetoothClassicPrinterTransport(context),
        NetworkPrinterTransport(),
    )
}

object AndroidPrinterPermissions {
    /** Runtime permissions needed to scan and connect Bluetooth printers on this OS version. */
    val bluetooth: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasBluetooth(context: Context): Boolean = bluetooth.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
}

/** System Bluetooth broadcasts come from the Bluetooth process, so the receiver must be exported on API 33+. */
internal fun Context.registerSystemReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        registerReceiver(receiver, filter)
    }
}
