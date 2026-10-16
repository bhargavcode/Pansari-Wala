package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Browser `history` bridge; a no-op on native targets where the OS owns back navigation. */
internal expect object BrowserHistory {
    val supported: Boolean
    fun push()
    fun go(delta: Int)
    /** Registers a `popstate` listener and returns its remover. */
    fun listen(onPop: () -> Unit): () -> Unit
}

private class HistoryTracker {
    var depth = 1
    var ignorePops = 0
}

/**
 * Mirrors an in-app back stack of [depth] entries into browser history so the browser back button
 * (and mouse/trackpad back gestures) pop one screen instead of leaving the app.
 */
@Composable
fun SyncBrowserHistory(depth: Int, onBrowserBack: () -> Unit) {
    if (!BrowserHistory.supported) return
    val onBack by rememberUpdatedState(onBrowserBack)
    val tracker = remember { HistoryTracker() }
    DisposableEffect(Unit) {
        val stop = BrowserHistory.listen {
            if (tracker.ignorePops > 0) {
                tracker.ignorePops--
            } else {
                tracker.depth = (tracker.depth - 1).coerceAtLeast(1)
                onBack()
            }
        }
        onDispose(stop)
    }
    SideEffect {
        val target = depth.coerceAtLeast(1)
        val diff = target - tracker.depth
        if (diff > 0) {
            repeat(diff) { BrowserHistory.push() }
        } else if (diff < 0) {
            tracker.ignorePops++
            BrowserHistory.go(diff)
        }
        tracker.depth = target
    }
}
