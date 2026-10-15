package org.bhargav.pansariwala.platform

import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Launches one activity-result request at a time under a stable [key] so the result survives
 * activity recreation (rotation, theme change, or the system reclaiming the activity while the
 * camera / gallery app is in front). The registry persists launched keys in saved state; the new
 * activity re-registers the same key via [reattach] and receives the pending result.
 */
internal object ActivityResultBridge {
    private class Request(
        val key: String,
        val contract: ActivityResultContract<Any?, Any?>,
        val onResult: (Result<Any?>) -> Unit,
    )

    private val main = Handler(Looper.getMainLooper())
    private var pending: Request? = null
    private var launcher: ActivityResultLauncher<Any?>? = null
    private var registeredOn: WeakReference<ComponentActivity>? = null

    suspend fun <I, O> launch(key: String, contract: ActivityResultContract<I, O>, input: I): O? =
        withContext(Dispatchers.Main.immediate) {
            val activity = AndroidActivityHolder.activity ?: return@withContext null
            suspendCancellableCoroutine { cont ->
                @Suppress("UNCHECKED_CAST")
                val request = Request(key, contract as ActivityResultContract<Any?, Any?>) { result ->
                    if (cont.isActive) {
                        result.fold(
                            onSuccess = { cont.resume(it as O?) },
                            onFailure = { cont.resumeWithException(it) },
                        )
                    }
                }
                start(activity, request, input)
                cont.invokeOnCancellation { main.post { if (pending === request) finish(request, null) } }
            }
        }

    @MainThread
    fun reattach(activity: ComponentActivity) {
        val request = pending ?: return
        if (registeredOn?.get() === activity) return
        register(activity, request)
    }

    @MainThread
    private fun start(activity: ComponentActivity, request: Request, input: Any?) {
        pending?.let { finish(it, Result.success(null)) }
        pending = request
        val current = register(activity, request) ?: return
        try {
            current.launch(input)
        } catch (e: Exception) {
            finish(request, Result.failure(e))
        }
    }

    @MainThread
    private fun register(activity: ComponentActivity, request: Request): ActivityResultLauncher<Any?>? {
        releaseLauncher()
        val registered = activity.activityResultRegistry.register(request.key, request.contract) { result ->
            if (pending === request) finish(request, Result.success(result))
        }
        // A restored result may already have been delivered synchronously inside register().
        if (pending !== request) {
            registered.unregister()
            return null
        }
        launcher = registered
        registeredOn = WeakReference(activity)
        return registered
    }

    @MainThread
    private fun finish(request: Request, result: Result<Any?>?) {
        pending = null
        releaseLauncher()
        result?.let(request.onResult)
    }

    private fun releaseLauncher() {
        launcher?.unregister()
        launcher = null
        registeredOn = null
    }
}
