package org.bhargav.pansariwala.media

import org.bhargav.pansariwala.media.ImageSource.CAMERA
import org.bhargav.pansariwala.media.ImageSource.GALLERY
import org.bhargav.pansariwala.util.AppConstants

enum class ImageSource { CAMERA, GALLERY }

/**
 * Per-feature image upload options. Edit [sources] to control what the user is offered:
 * one source opens that picker directly; two or more show a camera/gallery chooser sheet.
 */
enum class ImageUploadFeature(
    val prefix: String,
    val sources: List<ImageSource>,
) {
    PARTNER_REGISTER_PROFILE(AppConstants.S3Prefix.PARTNERS_USER_IMAGE, listOf(CAMERA)),
    PARTNER_REGISTER_DOCUMENT(AppConstants.S3Prefix.PARTNERS_USER_IDS, listOf(CAMERA)),
    PARTNER_REGISTER_VEHICLE(AppConstants.S3Prefix.PARTNERS_VEHICLE_IMAGE, listOf(CAMERA)),
    PARTNER_PROFILE_PHOTO(AppConstants.S3Prefix.PARTNERS_USER_IMAGE, listOf(CAMERA, GALLERY)),
    DELIVERY_PACKET(AppConstants.S3Prefix.SHOPS_DELIVERY_PACKETS, listOf(CAMERA)),
    INVENTORY_PRODUCT(AppConstants.S3Prefix.SHOPS_PRODUCT_IMAGES, listOf(CAMERA, GALLERY)),
    ;

    init {
        require(sources.isNotEmpty()) { "$name must allow at least one image source" }
    }
}
