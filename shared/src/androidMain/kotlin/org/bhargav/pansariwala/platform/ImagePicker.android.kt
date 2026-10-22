package org.bhargav.pansariwala.platform

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.media.normalizeImageMime
import org.bhargav.pansariwala.util.AppConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

/** Must match the `<provider>` authority declared in the app manifest. */
private const val FILE_PROVIDER_SUFFIX = ".pansari.fileprovider"
private const val CAMERA_CACHE_DIR = "camera"

/** Stable keys so results are re-delivered after activity recreation. */
private const val KEY_GALLERY = "pansari_image_gallery"
private const val KEY_GALLERY_FALLBACK = "pansari_image_gallery_fallback"
private const val KEY_CAMERA = "pansari_image_camera"
private const val KEY_CAMERA_PERMISSION = "pansari_image_camera_permission"

/** Files this size or smaller in a supported format are passed through untouched. */
private const val PASSTHROUGH_MAX_BYTES = 4_000_000
private const val TRANSCODE_JPEG_QUALITY = 92

class AndroidImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? {
        val context = AndroidActivityHolder.activity?.applicationContext ?: return null
        return when (source) {
            ImageSource.GALLERY -> pickFromGallery(context)
            ImageSource.CAMERA -> captureWithCamera(context)
        }
    }

    private suspend fun pickFromGallery(context: Context): PickedImage? {
        val uri = try {
            ActivityResultBridge.launch(
                KEY_GALLERY,
                ActivityResultContracts.PickVisualMedia(),
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        } catch (_: ActivityNotFoundException) {
            // Devices without the system photo picker or DocumentsUI (some Go / OEM builds).
            ActivityResultBridge.launch(KEY_GALLERY_FALLBACK, ActivityResultContracts.GetContent(), "image/*")
        } ?: return null
        return withContext(Dispatchers.IO) {
            wrapReadErrors { readFromUri(context, uri) }
        }
    }

    private suspend fun captureWithCamera(context: Context): PickedImage? {
        if (!ensureCameraPermission(context)) throw CameraPermissionDeniedException()
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
            // Leftovers from a capture interrupted by process death.
            dir.listFiles()?.forEach { it.delete() }
            File(dir, "capture_${UUID.randomUUID()}.jpg")
        }
        try {
            val uri = try {
                FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)
            } catch (e: IllegalArgumentException) {
                throw CameraUnavailableException(e)
            }
            try {
                ActivityResultBridge.launch(KEY_CAMERA, ActivityResultContracts.TakePicture(), uri)
            } catch (e: ActivityNotFoundException) {
                throw CameraUnavailableException(e)
            } catch (e: SecurityException) {
                throw CameraPermissionDeniedException()
            }
            return withContext(Dispatchers.IO) {
                // Trust the file, not the result code: some OEM camera apps report cancel after saving.
                if (file.length() == 0L) return@withContext null
                wrapReadErrors { readFromFile(file) }
            }
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }

    private suspend fun ensureCameraPermission(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return true
        val allowed = ActivityResultBridge.launch(
            KEY_CAMERA_PERMISSION,
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.CAMERA,
        ) == true
        if (!allowed) openAppSettingsIfPermanentlyDenied(context)
        return allowed
    }

    /** After "Don't ask again" the system dialog no longer appears, so send the user to Settings. */
    private suspend fun openAppSettingsIfPermanentlyDenied(context: Context) = withContext(Dispatchers.Main.immediate) {
        val activity = AndroidActivityHolder.activity ?: return@withContext
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) return@withContext
        runCatching {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        }
    }
}

private inline fun wrapReadErrors(block: () -> PickedImage): PickedImage =
    try {
        block()
    } catch (e: ImagePickException) {
        throw e
    } catch (e: OutOfMemoryError) {
        throw ImagePickException("Image is too large", e)
    } catch (e: Exception) {
        throw ImagePickException("Could not read photo", e)
    }

private fun readFromUri(context: Context, uri: Uri): PickedImage {
    val resolver = context.contentResolver
    val name = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: "photo.jpg"
    val mime = normalizeImageMime(resolver.getType(uri).orEmpty(), name)
    val open = { resolver.openInputStream(uri) ?: error("Could not open photo") }
    if (mime != null) {
        open().use { readUpTo(it, PASSTHROUGH_MAX_BYTES) }?.let {
            return PickedImage(displayName = name, bytes = it, mimeType = mime)
        }
    }
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeWithImageDecoder(ImageDecoder.createSource(resolver, uri))
    } else {
        decodeWithBitmapFactory(open)
    }
    return bitmap.toPickedJpeg(name)
}

private fun readFromFile(file: File): PickedImage {
    if (file.length() <= PASSTHROUGH_MAX_BYTES) {
        return PickedImage(displayName = file.name, bytes = file.readBytes(), mimeType = "image/jpeg")
    }
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeWithImageDecoder(ImageDecoder.createSource(file))
    } else {
        decodeWithBitmapFactory { file.inputStream() }
    }
    return bitmap.toPickedJpeg(file.name)
}

/** Returns all bytes, or null if the stream is longer than [limit]. */
private fun readUpTo(input: InputStream, limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        total += n
        if (total > limit) return null
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

/** API 28+: decodes HEIC/HEIF too and applies EXIF orientation automatically. */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodeWithImageDecoder(source: ImageDecoder.Source): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
        decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
    }

private fun decodeWithBitmapFactory(open: () -> InputStream): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ImagePickException("Unsupported image")
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = open().use { BitmapFactory.decodeStream(it, null, opts) }
        ?: throw ImagePickException("Unsupported image")
    val orientation = runCatching {
        open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return decoded.rotatedFor(orientation)
}

private fun sampleSizeFor(width: Int, height: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= AppConstants.PHOTO_MAX_EDGE_PX) sample *= 2
    return sample
}

private fun Bitmap.rotatedFor(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
        else -> return this
    }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated !== this) recycle()
    return rotated
}

private fun Bitmap.toPickedJpeg(sourceName: String): PickedImage {
    try {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, TRANSCODE_JPEG_QUALITY, out)
        val name = sourceName.substringBeforeLast('.').ifBlank { "photo" } + ".jpg"
        return PickedImage(displayName = name, bytes = out.toByteArray(), mimeType = "image/jpeg")
    } finally {
        recycle()
    }
}
