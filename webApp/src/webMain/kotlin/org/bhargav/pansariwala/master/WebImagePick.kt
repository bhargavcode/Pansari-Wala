package org.bhargav.pansariwala.master

import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.js.Promise

data class WebPickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

class WebImageDecodeException : IllegalStateException("Unsupported image format")

private external interface JsPickedImage : JsAny {
    val name: String
    val buffer: ArrayBuffer?
    val error: String?
}

/**
 * Opens the camera/gallery chooser (any image type so phones offer both) and re-encodes the picked image to a
 * downscaled JPEG in the browser, so iPhone HEIC photos and multi-MB camera shots upload reliably.
 * Returns null when the user cancels.
 */
suspend fun pickWebImageFile(): WebPickedFile? {
    val picked = pickImageAsJpeg(WEB_IMAGE_MAX_DIMENSION, WEB_IMAGE_JPEG_QUALITY).await<JsAny?>()
        ?.unsafeCast<JsPickedImage>() ?: return null
    if (picked.error != null) throw WebImageDecodeException()
    val buffer = picked.buffer ?: return null
    val baseName = picked.name.substringBeforeLast('.').ifBlank { "photo" }
    return WebPickedFile(name = "$baseName.jpg", mimeType = "image/jpeg", bytes = buffer.toByteArray())
}

suspend fun pickWebFile(accept: String, fallbackMime: String): WebPickedFile? = suspendCancellableCoroutine { cont ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.style.display = "none"
    // Safari drops change events on detached inputs.
    document.body?.appendChild(input)
    fun finish(file: WebPickedFile?) {
        input.remove()
        if (cont.isActive) cont.resume(file)
    }
    input.addEventListener("cancel", { finish(null) })
    input.onchange = {
        val file = input.files?.item(0)
        if (file == null) {
            finish(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val buffer = reader.result as? ArrayBuffer
                finish(buffer?.let { WebPickedFile(file.name, file.type.ifBlank { fallbackMime }, it.toByteArray()) })
            }
            reader.onerror = { finish(null) }
            reader.readAsArrayBuffer(file)
        }
    }
    input.click()
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val arr = Int8Array(this)
    return ByteArray(arr.length) { idx -> arr[idx] }
}

private fun pickImageAsJpeg(maxDim: Int, quality: Double): Promise<JsAny?> = js(
    """
    new Promise(function (resolve) {
      var input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.style.display = 'none';
      document.body.appendChild(input);
      var settled = false;
      var done = function (value) {
        if (settled) return;
        settled = true;
        if (input.parentNode) input.parentNode.removeChild(input);
        resolve(value);
      };
      input.addEventListener('cancel', function () { done(null); });
      input.addEventListener('change', function () {
        var file = input.files && input.files[0];
        if (!file) { done(null); return; }
        var url = URL.createObjectURL(file);
        var img = new Image();
        img.onload = function () {
          var w = img.naturalWidth, h = img.naturalHeight;
          var scale = Math.min(1, maxDim / Math.max(w, h));
          var canvas = document.createElement('canvas');
          canvas.width = Math.max(1, Math.round(w * scale));
          canvas.height = Math.max(1, Math.round(h * scale));
          var ctx = canvas.getContext('2d');
          ctx.fillStyle = '#ffffff';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          URL.revokeObjectURL(url);
          canvas.toBlob(function (blob) {
            if (!blob) { done({ name: file.name, buffer: null, error: 'encode' }); return; }
            blob.arrayBuffer().then(
              function (buf) { done({ name: file.name, buffer: buf, error: null }); },
              function () { done({ name: file.name, buffer: null, error: 'read' }); }
            );
          }, 'image/jpeg', quality);
        };
        img.onerror = function () {
          URL.revokeObjectURL(url);
          done({ name: file.name, buffer: null, error: 'decode' });
        };
        img.src = url;
      });
      input.click();
    })
    """,
)

private const val WEB_IMAGE_MAX_DIMENSION = 1600
private const val WEB_IMAGE_JPEG_QUALITY = 0.85
