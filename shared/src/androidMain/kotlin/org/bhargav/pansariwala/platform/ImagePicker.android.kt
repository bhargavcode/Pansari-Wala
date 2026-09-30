package org.bhargav.pansariwala.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.media.normalizeImageMime
import org.bhargav.pansariwala.util.AppConstants
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/** Must match the `<provider>` authority declared in the app manifest. */
private const val FILE_PROVIDER_SUFFIX = ".pansari.fileprovider"
private const val CAMERA_CACHE_DIR = "camera"

class AndroidImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? {
        val activity = AndroidActivityHolder.activity ?: return null
        return when (source) {
            ImageSource.GALLERY -> pickFromGallery(activity)
            ImageSource.CAMERA -> captureWithCamera(activity)
        }
    }

    private suspend fun pickFromGallery(activity: ComponentActivity): PickedImage? {
        val uri = launchForResult(
            activity,
            ActivityResultContracts.PickVisualMedia(),
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        ) ?: return null
        return withContext(Dispatchers.IO) { runCatching { readPicked(activity, uri) }.getOrNull() }
    }

    private suspend fun captureWithCamera(activity: ComponentActivity): PickedImage? {
        if (!ensureCameraPermission(activity)) return null
        val file = withContext(Dispatchers.IO) {
            File(activity.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
                .let { File(it, "capture_${UUID.randomUUID()}.jpg") }
        }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + FILE_PROVIDER_SUFFIX, file)
        val saved = launchForResult(activity, ActivityResultContracts.TakePicture(), uri) == true
        return withContext(Dispatchers.IO) {
            try {
                if (!saved || file.length() == 0L) return@withContext null
                require(file.length() <= AppConstants.UPLOAD_MAX_RAW_BYTES) { "Image is too large" }
                PickedImage(displayName = file.name, bytes = file.readBytes(), mimeType = "image/jpeg")
            } finally {
                file.delete()
            }
        }
    }

    private suspend fun ensureCameraPermission(activity: ComponentActivity): Boolean {
        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return true
        return launchForResult(
            activity,
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.CAMERA,
        ) == true
    }
}

private suspend fun <I, O> launchForResult(
    activity: ComponentActivity,
    contract: ActivityResultContract<I, O>,
    input: I,
): O? = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { cont ->
        val key = "pansari_image_${UUID.randomUUID()}"
        lateinit var launcher: ActivityResultLauncher<I>
        launcher = activity.activityResultRegistry.register(key, contract) { result ->
            launcher.unregister()
            if (cont.isActive) cont.resume(result)
        }
        cont.invokeOnCancellation { launcher.unregister() }
        runCatching { launcher.launch(input) }.onFailure {
            launcher.unregister()
            if (cont.isActive) cont.resume(null)
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
    return PickedImage(displayName = name, bytes = bytes, mimeType = mime)
}
