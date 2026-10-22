package org.bhargav.pansariwala.feature.udhaar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.BorrowerUpdateRequest
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.media.CompressedImage
import org.bhargav.pansariwala.media.ImageUploadFeature

/**
 * Saves a new borrower's details (photo, address) captured at the POS udhaar step.
 * Runs outside the order screen's lifecycle so closing the bill does not cancel the upload.
 */
class BorrowerProfileUploader(private val api: PansariApi) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun submit(phone: String, name: String, address: String, photo: CompressedImage?) {
        if (photo == null && address.isBlank() && name.isBlank()) return
        scope.launch {
            runCatching {
                val photoUrl = photo?.let {
                    api.uploadImage(
                        prefix = ImageUploadFeature.BORROWER_PHOTO.prefix,
                        fileName = it.displayName,
                        bytes = it.bytes,
                        contentType = it.mimeType,
                        thumbnailBytes = it.thumbnailBytes,
                    ).url
                }
                api.updateBorrower(
                    phone,
                    BorrowerUpdateRequest(name = name.trim(), address = address.trim(), photoUrl = photoUrl),
                )
            }.onFailure { it.rethrowIfStructuredCancellation() }
        }
    }
}
