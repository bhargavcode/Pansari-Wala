package org.bhargav.pansariwala.platform

internal actual object BrowserHistory {
    actual val supported: Boolean = false
    actual fun push() = Unit
    actual fun go(delta: Int) = Unit
    actual fun listen(onPop: () -> Unit): () -> Unit = {}
}
