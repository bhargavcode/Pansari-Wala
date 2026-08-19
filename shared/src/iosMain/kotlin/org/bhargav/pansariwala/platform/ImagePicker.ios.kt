package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import org.bhargav.pansariwala.util.AppConstants

class IosImagePicker : ImagePicker {
    private var delegate: ImagePickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun pickImage(): PickedImage? = withContext(Dispatchers.Main) {
        val presenter = topViewController() ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
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

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onPicked: (PickedImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        if (image == null) {
            onPicked(null)
            return
        }
        val quality = AppConstants.PHOTO_JPEG_QUALITY / 100.0
        val data = UIImageJPEGRepresentation(image, quality)
        val base64 = data?.base64EncodedStringWithOptions(0u)
        onPicked(
            if (base64.isNullOrBlank()) {
                null
            } else {
                PickedImage(displayName = "photo.jpg", base64 = base64)
            },
        )
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
