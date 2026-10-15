package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** iOS shows the Bluetooth prompt on first scan; after a denial only Settings can re-enable it. */
@Composable
actual fun rememberPrinterAccessRequester(onResult: (granted: Boolean) -> Unit): PrinterAccessRequester =
    remember {
        object : PrinterAccessRequester {
            override val canRequestEnableBluetooth: Boolean = false

            override fun requestBluetoothPermission() {
                val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
                UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
            }

            override fun requestEnableBluetooth() = requestBluetoothPermission()
        }
    }
