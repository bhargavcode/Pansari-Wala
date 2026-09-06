package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.media.PickedImage
import platform.Foundation.NSData
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
import platform.posix.memcpy
import kotlin.coroutines.resume

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
        // High-quality capture; shared compressor does WhatsApp-style reduction before upload.
        val data = UIImageJPEGRepresentation(image, 0.95)
        val bytes = data?.toByteArray()
        onPicked(
            if (bytes == null || bytes.isEmpty()) {
                null
            } else {
                PickedImage(displayName = "photo.jpg", bytes = bytes, mimeType = "image/jpeg")
            },
        )
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onPicked(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, size.toULong())
    }
    return out
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.windows.mapNotNull { it as? UIWindow }.firstOrNull { it.isKeyWindow() }
    return topMost(window?.rootViewController)
}

private fun topMost(controller: UIViewController?): UIViewController? {
    val presented = controller?.presentedViewController
    return if (presented != null) topMost(presented) else controller
}
