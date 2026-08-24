package org.bhargav.pansariwala

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.landing.LandingApp
import org.bhargav.pansariwala.master.MasterAdminApp
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProductHolder
import org.bhargav.pansariwala.util.AppConstants

private var koinStarted = false

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (!koinStarted) {
        val host = window.location.hostname
        ApiRuntime.baseUrl = if (host == "localhost" || host == "127.0.0.1") {
            AppConstants.LOCAL_API_BASE_URL
        } else {
            AppConstants.API_BASE_URL
        }
        initKoin()
        koinStarted = true
    }
    val route = currentWebRoute()
    ComposeViewport {
        when (route) {
            WebRoute.Landing -> LandingApp()
            WebRoute.MasterAdmin -> MasterAdminApp()
            WebRoute.UserApp -> {
                AppProductHolder.current = AppProduct.USER
                App()
            }
        }
    }
}
