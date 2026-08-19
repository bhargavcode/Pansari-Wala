package org.bhargav.pansariwala.platform

data class PickedImage(
    val displayName: String,
    val base64: String,
)

interface ImagePicker {
    suspend fun pickImage(): PickedImage?
}

class UnavailableImagePicker : ImagePicker {
    override suspend fun pickImage(): PickedImage? = null
}
