package org.bhargav.pansariwala.platform

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val UPDATE_REQUEST_CODE = 7311

@Volatile
private var pendingUpdate: AppUpdateInfo? = null

actual fun requestInAppReview() {
    val activity = AndroidActivityHolder.activity ?: return
    val manager = ReviewManagerFactory.create(activity)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
    }
}

actual suspend fun isStoreUpdateAvailable(): Boolean {
    val activity = AndroidActivityHolder.activity ?: return false
    val manager = AppUpdateManagerFactory.create(activity)
    return suspendCancellableCoroutine { cont ->
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                pendingUpdate = info.takeIf { available }
                if (cont.isActive) cont.resume(available)
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(false) }
    }
}

actual fun startStoreUpdate() {
    val activity = AndroidActivityHolder.activity ?: return
    val info = pendingUpdate ?: return
    @Suppress("DEPRECATION")
    AppUpdateManagerFactory.create(activity).startUpdateFlowForResult(
        info,
        activity,
        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
        UPDATE_REQUEST_CODE,
    )
}
