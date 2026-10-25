package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/** Browser `history` bridge; a no-op on native targets where the OS owns back navigation. */
internal expect object BrowserHistory {
    val supported: Boolean
    fun push()
    fun go(delta: Int)
    /** Registers a `popstate` listener and returns its remover. */
    fun listen(onPop: () -> Unit): () -> Unit
    /** Runs [block] once on the first pointer/key press (browsers skip history entries added before that). */
    fun onFirstInteraction(block: () -> Unit)
}

/**
 * Turns the browser back button (and mouse/trackpad back gestures) into an in-app back press: it goes to the
 * topmost back handler (sheet, dialog, keyboard, nav stack) exactly like Android back, and never leaves the
 * app because a guard history entry is re-pushed after every pop.
 */
@Composable
fun BrowserBackBridge() {
    if (!BrowserHistory.supported) return
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    DisposableEffect(dispatcher) {
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        BrowserHistory.push()
        BrowserHistory.onFirstInteraction { BrowserHistory.push() }
        val stop = BrowserHistory.listen {
            BrowserHistory.push()
            input.backCompleted()
        }
        onDispose {
            stop()
            dispatcher.removeInput(input)
        }
    }
}
