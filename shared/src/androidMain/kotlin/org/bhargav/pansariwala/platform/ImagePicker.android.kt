package org.bhargav.pansariwala.platform

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.media.normalizeImageMime
import org.bhargav.pansariwala.util.AppConstants
import java.util.UUID
import kotlin.coroutines.resume

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
    val mimeRaw = activity.contentResolver.getType(uri).orEmpty()
    val mime = normalizeImageMime(mimeRaw, name)
        ?: error("Only PNG, JPEG, or WebP images are allowed")
    val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Could not read photo")
    require(bytes.size <= AppConstants.UPLOAD_MAX_RAW_BYTES) { "Image is too large" }
    // Validate decodeable
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Could not decode photo")
    return PickedImage(displayName = name, bytes = bytes, mimeType = mime)
}
