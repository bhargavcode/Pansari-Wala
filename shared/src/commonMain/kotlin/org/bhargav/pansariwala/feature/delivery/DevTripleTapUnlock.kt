package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants

/**
 * DEV ONLY — triple-tap unlock for arrival testing.
 *
 * To remove: delete this file, drop [devTripleTapToUnlock] usages,
 * and remove DEV_ARRIVAL_UNLOCK_* from AppConstants.
 */
class DevTripleTapUnlock(
    private val tapsRequired: Int = AppConstants.DEV_ARRIVAL_UNLOCK_TAPS,
    private val windowMs: Long = AppConstants.DEV_ARRIVAL_UNLOCK_WINDOW_MS,
) {
    private val taps = ArrayDeque<Long>()

    /** @return true when unlock condition is met */
    fun registerTap(nowMs: Long = AppClock.nowMillis()): Boolean {
        taps.addLast(nowMs)
        while (taps.isNotEmpty() && nowMs - taps.first() > windowMs) {
            taps.removeFirst()
        }
        if (taps.size < tapsRequired) return false
        taps.clear()
        return true
    }
}

fun Modifier.devTripleTapToUnlock(onUnlocked: () -> Unit): Modifier = composed {
    val unlock = remember { DevTripleTapUnlock() }
    pointerInput(Unit) {
        detectTapGestures {
            if (unlock.registerTap()) onUnlocked()
        }
    }
}
