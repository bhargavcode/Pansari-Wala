package org.bhargav.pansariwala.platform

import kotlinx.browser.window
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event
import kotlin.js.json

internal actual object BrowserHistory {
    actual val supported: Boolean = true

    private var leavePrompt = false
    private var unloadHooked = false

    actual fun push(index: Int) = window.history.pushState(json(STATE_KEY to index), "")

    actual fun go(delta: Int) = window.history.go(delta)

    actual fun listen(onPop: (Int) -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { event ->
            val state = (event as? PopStateEvent)?.state?.asDynamic()
            onPop((state?.get(STATE_KEY) as? Int) ?: 0)
        }
        window.addEventListener("popstate", handler)
        return { window.removeEventListener("popstate", handler) }
    }

    actual fun onInteraction(block: () -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { block() }
        ACTIVATION_EVENTS.forEach { window.addEventListener(it, handler) }
        return { ACTIVATION_EVENTS.forEach { window.removeEventListener(it, handler) } }
    }

    actual fun replaceLocation(path: String) = window.location.replace(path)

    actual fun setLeavePrompt(enabled: Boolean) {
        leavePrompt = enabled
        if (unloadHooked || !enabled) return
        unloadHooked = true
        window.addEventListener("beforeunload", { event: Event ->
            if (leavePrompt) {
                event.preventDefault()
                event.asDynamic().returnValue = ""
            }
        })
    }
}

private const val STATE_KEY = "pwGuard"
private val ACTIVATION_EVENTS = listOf("pointerup", "mousedown", "touchend", "keydown")
