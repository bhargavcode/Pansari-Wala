package org.bhargav.pansariwala.platform

private fun historyPush(): Unit = js("window.history.pushState(null, '')")

private fun historyGo(delta: Int): Unit = js("window.history.go(delta)")

private fun addPopListener(callback: () -> Unit): JsAny =
    js("{ const h = () => callback(); window.addEventListener('popstate', h); return h; }")

private fun removePopListener(handler: JsAny): Unit = js("window.removeEventListener('popstate', handler)")

private fun addFirstInteraction(callback: () -> Unit): Unit =
    js("{ let f = false; const h = () => { if (!f) { f = true; callback(); } }; window.addEventListener('pointerdown', h); window.addEventListener('keydown', h); }")

internal actual object BrowserHistory {
    actual val supported: Boolean = true

    actual fun push() = historyPush()

    actual fun go(delta: Int) = historyGo(delta)

    actual fun listen(onPop: () -> Unit): () -> Unit {
        val handler = addPopListener(onPop)
        return { removePopListener(handler) }
    }

    actual fun onFirstInteraction(block: () -> Unit) = addFirstInteraction(block)
}
