package org.bhargav.pansariwala.platform

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.web_exit_cancel
import pansariwala.shared.generated.resources.web_exit_confirm
import pansariwala.shared.generated.resources.web_exit_message
import pansariwala.shared.generated.resources.web_exit_title

/** Browser `history` bridge; a no-op on native targets where the OS owns back navigation. */
internal expect object BrowserHistory {
    val supported: Boolean
    fun push()
    fun go(delta: Int)
    /** Registers a `popstate` listener and returns its remover. */
    fun listen(onPop: () -> Unit): () -> Unit
    /** Runs [block] on every user-activation event (click/tap/key) and returns its remover. */
    fun onInteraction(block: () -> Unit): () -> Unit
    fun replaceLocation(path: String)
}

private object BackGuard {
    /** Chrome skips history entries pushed without user activation, so the guard is only "real" once re-armed on input. */
    var armed = false
    var exiting = false
    var pops = 0
}

/**
 * Turns the browser back button (and mouse/trackpad back gestures) into an in-app back press: it goes to the
 * topmost back handler (sheet, dialog, keyboard, nav stack) exactly like Android back. A guard history entry
 * is re-pushed after each pop and again on the next user interaction, so the browser never leaves the app.
 */
@Composable
fun BrowserBackBridge() {
    if (!BrowserHistory.supported) return
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    DisposableEffect(dispatcher) {
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        BrowserHistory.push()
        val stopInteraction = BrowserHistory.onInteraction {
            if (!BackGuard.armed && !BackGuard.exiting) {
                BackGuard.armed = true
                BrowserHistory.push()
            }
        }
        val stopPop = BrowserHistory.listen {
            BackGuard.pops++
            if (BackGuard.exiting) {
                BrowserHistory.go(-1)
            } else {
                BackGuard.armed = false
                BrowserHistory.push()
                input.backCompleted()
            }
        }
        onDispose {
            stopPop()
            stopInteraction()
            dispatcher.removeInput(input)
        }
    }
}

/**
 * Lowest-priority back handler for web: reached only when nothing else (sheet, tab, nav stack) consumes back,
 * i.e. on the home screen. Asks before leaving the app. Must be composed before the nav graph.
 */
@Composable
fun WebExitGuard() {
    if (!BrowserHistory.supported) return
    var asking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { asking = true },
    )
    if (!asking) return
    AppAlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(stringResource(Res.string.web_exit_title)) },
        text = { Text(stringResource(Res.string.web_exit_message)) },
        confirmButton = {
            TextButton(onClick = {
                asking = false
                BackGuard.exiting = true
                BrowserHistory.go(-1)
                scope.launch {
                    var seen = -1
                    while (seen != BackGuard.pops) {
                        seen = BackGuard.pops
                        delay(AppConstants.WEB_EXIT_SETTLE_MS)
                    }
                    BrowserHistory.replaceLocation(AppConstants.WEB_LANDING_PATH)
                }
            }) { Text(stringResource(Res.string.web_exit_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { asking = false }) { Text(stringResource(Res.string.web_exit_cancel)) }
        },
    )
}

private class HistoryTracker {
    var depth = 1
    var ignorePops = 0
}

/** Mirrors an app back stack of [depth] entries into browser history (used by the master web console). */
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
        if (diff > 0) repeat(diff) { BrowserHistory.push() } else if (diff < 0) {
            tracker.ignorePops++
            BrowserHistory.go(diff)
        }
        tracker.depth = target
    }
}
