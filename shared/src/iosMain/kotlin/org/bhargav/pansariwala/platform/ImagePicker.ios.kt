package org.bhargav.pansariwala.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.media.jpegBytes
import org.bhargav.pansariwala.media.resizeImage
import org.bhargav.pansariwala.util.AppConstants
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

class IosImagePicker : ImagePicker {
    private var delegate: ImagePickerDelegate? = null

    override suspend fun pickImage(source: ImageSource): PickedImage? {
        val image = presentPicker(source) ?: return null
        // Downscale off the main thread; the full-res capture is only ever touched once here.
        return withContext(Dispatchers.Default) {
            val resized = resizeImage(image, AppConstants.PHOTO_MAX_EDGE_PX)
            val bytes = jpegBytes(resized, AppConstants.PHOTO_JPEG_QUALITY / 100.0)
            if (bytes == null || bytes.isEmpty()) null
            else PickedImage(displayName = "photo.jpg", bytes = bytes, mimeType = "image/jpeg")
        }
    }

    private suspend fun presentPicker(source: ImageSource): UIImage? = withContext(Dispatchers.Main) {
        val presenter = topViewController() ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            val picker = UIImagePickerController()
            picker.sourceType = resolveSourceType(source)
            picker.allowsEditing = false
            val nextDelegate = ImagePickerDelegate { picked ->
                delegate = null
                if (cont.isActive) cont.resume(picked)
            }
            delegate = nextDelegate
            picker.delegate = nextDelegate
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/** Simulator has no camera: fall back to the library instead of crashing. */
private fun resolveSourceType(source: ImageSource): UIImagePickerControllerSourceType {
    val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    val library = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
    return if (source == ImageSource.CAMERA && UIImagePickerController.isSourceTypeAvailable(camera)) camera else library
}

private class ImagePickerDelegate(
    private val onPicked: (UIImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onPicked(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onPicked(null)
    }
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.windows.mapNotNull { it as? UIWindow }.firstOrNull { it.isKeyWindow() }
    return topMost(window?.rootViewController)
}

private fun topMost(controller: UIViewController?): UIViewController? {
    val presented = controller?.presentedViewController
    return if (presented != null) topMost(presented) else controller
}
