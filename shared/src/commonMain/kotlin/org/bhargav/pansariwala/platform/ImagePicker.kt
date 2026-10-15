package org.bhargav.pansariwala.platform

import kotlinx.coroutines.CancellationException
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage

interface ImagePicker {
    /** Returns null when the user cancels; throws [ImagePickException] on real failures. */
    suspend fun pickImage(source: ImageSource = ImageSource.GALLERY): PickedImage?
}

open class ImagePickException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class CameraPermissionDeniedException : ImagePickException("Camera permission denied")

class UnavailableImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? = null
}

/** Success(null) = user cancelled; failure = something to show the user. */
suspend fun ImagePicker.pickImageCatching(source: ImageSource): Result<PickedImage?> =
    try {
        Result.success(pickImage(source))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
