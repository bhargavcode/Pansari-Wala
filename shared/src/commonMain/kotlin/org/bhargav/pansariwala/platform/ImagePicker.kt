package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.media.PickedImage

interface ImagePicker {
    suspend fun pickImage(): PickedImage?
}

class UnavailableImagePicker : ImagePicker {
    override suspend fun pickImage(): PickedImage? = null
}
