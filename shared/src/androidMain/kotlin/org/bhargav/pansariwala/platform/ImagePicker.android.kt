package org.bhargav.pansariwala.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.util.AppConstants
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.max

class AndroidImagePicker : ImagePicker {
    override suspend fun pickImage(): PickedImage? {
        val activity = AndroidActivityHolder.activity ?: return null
        return suspendCancellableCoroutine { cont ->
            val key = "pick_image_${UUID.randomUUID()}"
            lateinit var launcher: ActivityResultLauncher<PickVisualMediaRequest>
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                launcher.unregister()
                if (uri == null) {
                    cont.resume(null)
                } else {
                    cont.resume(runCatching { readPicked(activity, uri) }.getOrNull())
                }
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

private fun readPicked(activity: ComponentActivity, uri: Uri): PickedImage {
    val name = activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "photo.jpg"
    val original = activity.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: error("Could not read photo")
    val scaled = scaleDown(original, AppConstants.PHOTO_MAX_EDGE_PX)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, AppConstants.PHOTO_JPEG_QUALITY, out)
    if (scaled !== original) scaled.recycle()
    original.recycle()
    return PickedImage(
        displayName = name,
        base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP),
    )
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
