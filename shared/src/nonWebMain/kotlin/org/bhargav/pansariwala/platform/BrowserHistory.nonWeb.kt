package org.bhargav.pansariwala.platform

internal actual object BrowserHistory {
    actual val supported: Boolean = false
    actual fun push(index: Int) = Unit
    actual fun go(delta: Int) = Unit
    actual fun listen(onPop: (Int) -> Unit): () -> Unit = {}
    actual fun onInteraction(block: () -> Unit): () -> Unit = {}
    actual fun replaceLocation(path: String) = Unit
    actual fun setLeavePrompt(enabled: Boolean) = Unit
}
