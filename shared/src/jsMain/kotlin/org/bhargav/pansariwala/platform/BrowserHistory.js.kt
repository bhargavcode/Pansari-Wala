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

    actual fun onInteraction(block: () -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { block() }
        ACTIVATION_EVENTS.forEach { window.addEventListener(it, handler) }
        return { ACTIVATION_EVENTS.forEach { window.removeEventListener(it, handler) } }
    }

    actual fun replaceLocation(path: String) = window.location.replace(path)
}

private val ACTIVATION_EVENTS = listOf("pointerup", "mousedown", "touchend", "keydown")
