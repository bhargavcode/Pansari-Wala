package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable

/** Platform hooks for Bluetooth permission / radio prompts used by printer settings. */
interface PrinterAccessRequester {
    val canRequestEnableBluetooth: Boolean
    fun requestBluetoothPermission()
    fun requestEnableBluetooth()
}

/** [onResult] receives true when the user granted access or turned Bluetooth on. */
@Composable
expect fun rememberPrinterAccessRequester(onResult: (granted: Boolean) -> Unit): PrinterAccessRequester

internal object NoOpPrinterAccessRequester : PrinterAccessRequester {
    override val canRequestEnableBluetooth: Boolean = false
    override fun requestBluetoothPermission() = Unit
    override fun requestEnableBluetooth() = Unit
}
