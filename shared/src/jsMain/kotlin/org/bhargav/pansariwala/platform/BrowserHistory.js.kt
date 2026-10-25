package org.bhargav.pansariwala.platform

import kotlinx.browser.window
import org.w3c.dom.events.Event

internal actual object BrowserHistory {
    actual val supported: Boolean = true

    actual fun push() = window.history.pushState(null, "")

    actual fun go(delta: Int) = window.history.go(delta)

    actual fun listen(onPop: () -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { onPop() }
        window.addEventListener("popstate", handler)
        return { window.removeEventListener("popstate", handler) }
    }

    actual fun onFirstInteraction(block: () -> Unit) {
        var fired = false
        val handler: (Event) -> Unit = { if (!fired) { fired = true; block() } }
        window.addEventListener("pointerdown", handler)
        window.addEventListener("keydown", handler)
    }
}
