package org.bhargav.pansariwala.printer.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64

/**
 * 1-bit raster ready for thermal printing. Rows are packed MSB-first; a set bit is a black dot.
 */
@Serializable
data class MonoBitmap(
    val width: Int,
    val height: Int,
    @Serializable(with = Base64ByteArraySerializer::class)
    val data: ByteArray,
) {
    val bytesPerRow: Int get() = (width + 7) / 8

    fun isBlack(x: Int, y: Int): Boolean {
        val byte = data[y * bytesPerRow + x / 8].toInt()
        return byte and (0x80 ushr (x % 8)) != 0
    }

    override fun equals(other: Any?): Boolean =
        other is MonoBitmap && width == other.width && height == other.height && data.contentEquals(other.data)

    override fun hashCode(): Int = 31 * (31 * width + height) + data.contentHashCode()
}

/** Decoded image in ARGB_8888 (0xAARRGGBB), the platform-neutral input for [org.bhargav.pansariwala.printer.image.ImageProcessor]. */
class ArgbImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(pixels.size == width * height) { "pixels must be width * height" }
    }
}

internal object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ByteArray) = encoder.encodeString(Base64.encode(value))
    override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}
