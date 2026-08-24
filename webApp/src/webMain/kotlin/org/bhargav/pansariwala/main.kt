package org.bhargav.pansariwala

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bhargav.pansariwala.di.initKoin
import org.bhargav.pansariwala.landing.LandingApp
import org.bhargav.pansariwala.master.MasterAdminApp
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProductHolder

private var koinStarted = false

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (!koinStarted) {
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
