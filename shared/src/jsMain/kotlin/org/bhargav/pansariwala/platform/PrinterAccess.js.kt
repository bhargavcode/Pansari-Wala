package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPrinterAccessRequester(onResult: (granted: Boolean) -> Unit): PrinterAccessRequester =
    NoOpPrinterAccessRequester
