package org.bhargav.pansariwala.platform

private fun historyPush(index: Int): Unit = js("window.history.pushState({ pwGuard: index }, '')")

private fun historyGo(delta: Int): Unit = js("window.history.go(delta)")

private fun addPopListener(callback: (Int) -> Unit): JsAny =
    js("{ const h = (e) => callback((e.state && typeof e.state.pwGuard === 'number') ? e.state.pwGuard : 0); window.addEventListener('popstate', h); return h; }")

private fun removePopListener(handler: JsAny): Unit = js("window.removeEventListener('popstate', handler)")

private fun addActivationListener(callback: () -> Unit): JsAny =
    js("{ const h = () => callback(); ['pointerup', 'mousedown', 'touchend', 'keydown'].forEach(e => window.addEventListener(e, h)); return h; }")

private fun removeActivationListener(handler: JsAny): Unit =
    js("['pointerup', 'mousedown', 'touchend', 'keydown'].forEach(e => window.removeEventListener(e, handler))")

private fun locationReplace(path: String): Unit = js("window.location.replace(path)")

private fun setUnloadPrompt(enabled: Boolean): Unit = js(
    "{ window.__pwLeavePrompt = enabled; if (!window.__pwUnloadHooked) { window.__pwUnloadHooked = true; " +
        "window.addEventListener('beforeunload', (e) => { if (window.__pwLeavePrompt) { e.preventDefault(); e.returnValue = ''; } }); } }",
)

internal actual object BrowserHistory {
    actual val supported: Boolean = true

    actual fun push(index: Int) = historyPush(index)

    actual fun go(delta: Int) = historyGo(delta)

    actual fun listen(onPop: (Int) -> Unit): () -> Unit {
        val handler = addPopListener(onPop)
        return { removePopListener(handler) }
    }

    actual fun onInteraction(block: () -> Unit): () -> Unit {
        val handler = addActivationListener(block)
        return { removeActivationListener(handler) }
    }

    actual fun replaceLocation(path: String) = locationReplace(path)

    actual fun setLeavePrompt(enabled: Boolean) = setUnloadPrompt(enabled)
}
