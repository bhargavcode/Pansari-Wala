package org.bhargav.pansariwala

import androidx.compose.ui.window.ComposeUIViewController
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.util.AppConstants

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        ApiRuntime.baseUrl = AppConstants.IOS_API_BASE_URL
        initKoin()
        koinStarted = true
    }
    App()
}
