package org.bhargav.pansariwala.platform

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    /** Pushes a same-URL entry tagged with guard [index] (the page's own entry is index 0). */
    fun push(index: Int)
    fun go(delta: Int)
    /** Registers a `popstate` listener (receives the entry's guard index) and returns its remover. */
    fun listen(onPop: (Int) -> Unit): () -> Unit
    /** Runs [block] on every user-activation event (click/tap/key) and returns its remover. */
    fun onInteraction(block: () -> Unit): () -> Unit
    fun replaceLocation(path: String)
    /** Asks the browser to confirm before unloading the page (reload, close, back past the app). */
    fun setLeavePrompt(enabled: Boolean)
}

private object BackGuard {
    /** Guard index of the current history entry. */
    var index = 0
    var exiting = false
    var pops = 0
}

/** Lets the page unload without the browser's leave prompt (logout to landing, app switch reloads). */
fun allowBrowserLeave() {
    if (!BrowserHistory.supported) return
    BackGuard.exiting = true
    BrowserHistory.setLeavePrompt(false)
}

/**
 * Turns the browser back button (and mouse/trackpad back gestures) into an in-app back press: it goes to the
 * topmost back handler (sheet, dialog, keyboard, nav stack) exactly like Android back.
 *
 * Chrome's back button skips entries pushed without user activation, so guard entries are only ever pushed
 * from click/tap/key events (topped up to [AppConstants.WEB_BACK_GUARDS]) and never from `popstate`.
 */
@Composable
fun BrowserBackBridge() {
    if (!BrowserHistory.supported) return
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    DisposableEffect(dispatcher) {
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val stopInteraction = BrowserHistory.onInteraction {
            if (BackGuard.exiting) return@onInteraction
            BrowserHistory.setLeavePrompt(true)
            while (BackGuard.index < AppConstants.WEB_BACK_GUARDS) {
                BackGuard.index++
                BrowserHistory.push(BackGuard.index)
            }
        }
        val stopPop = BrowserHistory.listen { index ->
            BackGuard.pops++
            val wentBack = index < BackGuard.index
            BackGuard.index = index
            if (!BackGuard.exiting && wentBack) input.backCompleted()
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
                allowBrowserLeave()
                BrowserHistory.go(-(BackGuard.index + 1))
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