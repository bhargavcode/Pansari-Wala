package org.bhargav.pansariwala.platform

private fun historyPush(): Unit = js("window.history.pushState(null, '')")

private fun historyGo(delta: Int): Unit = js("window.history.go(delta)")

private fun addPopListener(callback: () -> Unit): JsAny =
    js("{ const h = () => callback(); window.addEventListener('popstate', h); return h; }")

private fun removePopListener(handler: JsAny): Unit = js("window.removeEventListener('popstate', handler)")

private fun addActivationListener(callback: () -> Unit): JsAny =
    js("{ const h = () => callback(); ['pointerup', 'mousedown', 'touchend', 'keydown'].forEach(e => window.addEventListener(e, h)); return h; }")

private fun removeActivationListener(handler: JsAny): Unit =
    js("['pointerup', 'mousedown', 'touchend', 'keydown'].forEach(e => window.removeEventListener(e, handler))")

private fun locationReplace(path: String): Unit = js("window.location.replace(path)")

internal actual object BrowserHistory {
    actual val supported: Boolean = true

    actual fun push() = historyPush()

    actual fun go(delta: Int) = historyGo(delta)

    actual fun listen(onPop: () -> Unit): () -> Unit {
        val handler = addPopListener(onPop)
        return { removePopListener(handler) }
    }

    actual fun onInteraction(block: () -> Unit): () -> Unit {
        val handler = addActivationListener(block)
        return { removeActivationListener(handler) }
    }

    actual fun replaceLocation(path: String) = locationReplace(path)
}
