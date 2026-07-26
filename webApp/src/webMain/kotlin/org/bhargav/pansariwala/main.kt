package org.bhargav.pansariwala

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bhargav.pansariwala.di.initKoin

private var koinStarted = false

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    ComposeViewport {
        App()
    }
}
