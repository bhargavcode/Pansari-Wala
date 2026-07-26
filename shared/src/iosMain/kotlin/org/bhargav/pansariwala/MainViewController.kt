package org.bhargav.pansariwala

import androidx.compose.ui.window.ComposeUIViewController
import org.bhargav.pansariwala.di.initKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    App()
}
