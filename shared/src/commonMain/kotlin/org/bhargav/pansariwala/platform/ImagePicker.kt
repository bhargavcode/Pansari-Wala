package org.bhargav.pansariwala.platform

import kotlinx.coroutines.CancellationException
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.i18n.UiText
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_camera_permission_denied
import pansariwala.shared.generated.resources.error_camera_unavailable
import pansariwala.shared.generated.resources.error_photo_pick_failed

interface ImagePicker {
    /** Returns null when the user cancels; throws [ImagePickException] on real failures. */
    suspend fun pickImage(source: ImageSource = ImageSource.GALLERY): PickedImage?
}

open class ImagePickException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class CameraPermissionDeniedException : ImagePickException("Camera permission denied")

class CameraUnavailableException(cause: Throwable? = null) : ImagePickException("No camera app available", cause)

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

fun Throwable.toImagePickErrorText(): UiText = UiText.res(
    when (this) {
        is CameraPermissionDeniedException -> Res.string.error_camera_permission_denied
        is CameraUnavailableException -> Res.string.error_camera_unavailable
        else -> Res.string.error_photo_pick_failed
    },
)
