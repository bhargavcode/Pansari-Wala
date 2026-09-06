package org.bhargav.pansariwala.media

/**
 * Platform image compressor (WhatsApp-style): downscale longest edge, encode JPEG,
 * iteratively lower quality until under target size (~90% reduction on large camera shots).
 */
expect fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage
