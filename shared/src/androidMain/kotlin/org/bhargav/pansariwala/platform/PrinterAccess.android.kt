package org.bhargav.pansariwala.platform

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.bhargav.pansariwala.printer.AndroidPrinterPermissions

@Composable
actual fun rememberPrinterAccessRequester(onResult: (granted: Boolean) -> Unit): PrinterAccessRequester {
    val callback = rememberUpdatedState(onResult)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results -> callback.value(results.isNotEmpty() && results.values.all { it }) }
    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> callback.value(result.resultCode == Activity.RESULT_OK) }

    return remember(permissionLauncher, enableLauncher) {
        object : PrinterAccessRequester {
            override val canRequestEnableBluetooth: Boolean = true

            override fun requestBluetoothPermission() {
                permissionLauncher.launch(AndroidPrinterPermissions.bluetooth)
            }

            override fun requestEnableBluetooth() {
                runCatching { enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                    .onFailure { callback.value(false) }
            }
        }
    }
}
