package org.bhargav.pansariwala

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.util.AppConstants

private var koinStarted = false

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        ApiRuntime.baseUrl = if (Platform.isDebugBinary) {
            AppConstants.LOCAL_API_BASE_URL
        } else {
            AppConstants.API_BASE_URL
        }
        initKoin()
        koinStarted = true
    }
    App()
}
