package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage

interface ImagePicker {
    suspend fun pickImage(source: ImageSource = ImageSource.GALLERY): PickedImage?
}

class UnavailableImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? = null
}
