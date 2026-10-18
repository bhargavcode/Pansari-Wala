package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeSpreadsheet
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

class IosDocumentPicker : DocumentPicker {
    private var delegate: DocumentPickerDelegate? = null

    override suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument? {
        val url = presentPicker(extensions) ?: return null
        return withContext(Dispatchers.Default) {
            val access = url.startAccessingSecurityScopedResource()
            try {
                val data = NSData.dataWithContentsOfURL(url) ?: error("Could not open file")
                PickedDocument(url.lastPathComponent ?: "products.xlsx", data.toBytes())
            } finally {
                if (access) url.stopAccessingSecurityScopedResource()
            }
        }
    }

    private suspend fun presentPicker(extensions: String): NSURL? = withContext(Dispatchers.Main) {
        val presenter = topViewController() ?: return@withContext null
        val types = listOf(UTTypeSpreadsheet, UTTypeCommaSeparatedText) +
            extensions.split(',').mapNotNull { UTType.typeWithFilenameExtension(it.trim().removePrefix(".")) }
        suspendCancellableCoroutine { cont ->
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
            val next = DocumentPickerDelegate { picked ->
                delegate = null
                if (cont.isActive) cont.resume(picked)
            }
            delegate = next
            picker.delegate = next
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class DocumentPickerDelegate(private val onPicked: (NSURL?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onPicked(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toBytes(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
