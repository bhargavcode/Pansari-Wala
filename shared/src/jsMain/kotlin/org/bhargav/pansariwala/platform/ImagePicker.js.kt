package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.util.AppConstants
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JsImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? = suspendCancellableCoroutine { cont ->
        val onDone: (dynamic) -> Unit = { bytes ->
            if (cont.isActive) {
                // Kotlin/JS ByteArray is an Int8Array, so the browser buffer is used as-is.
                cont.resume(if (bytes == null) null else PickedImage("photo.jpg", bytes.unsafeCast<ByteArray>(), "image/jpeg"))
            }
        }
        val onError: () -> Unit = {
            if (cont.isActive) cont.resumeWithException(ImagePickException("Could not read photo"))
        }
        openBrowserImagePicker(
            source == ImageSource.CAMERA,
            AppConstants.PHOTO_MAX_EDGE_PX,
            AppConstants.PHOTO_JPEG_QUALITY / 100.0,
            onDone,
            onError,
        )
    }
}

/** Hidden `<input type=file>` + canvas downscale to JPEG; EXIF orientation is applied by the browser. */
private val openBrowserImagePicker: dynamic = js(
    """(function (capture, maxEdge, quality, onDone, onError) {
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    if (capture) input.setAttribute('capture', 'environment');
    input.style.display = 'none';
    var settled = false;
    var done = function (ok, value) {
        if (settled) return;
        settled = true;
        input.remove();
        ok ? onDone(value) : onError();
    };
    var loadImage = function (file) {
        return new Promise(function (resolve, reject) {
            var url = URL.createObjectURL(file);
            var img = new Image();
            img.onload = function () { URL.revokeObjectURL(url); resolve(img); };
            img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('decode')); };
            img.src = url;
        });
    };
    input.addEventListener('cancel', function () { done(true, null); });
    input.addEventListener('change', function () {
        var file = input.files && input.files[0];
        if (!file) { done(true, null); return; }
        var decode = (typeof createImageBitmap === 'function')
            ? createImageBitmap(file, { imageOrientation: 'from-image' }).catch(function () { return loadImage(file); })
            : loadImage(file);
        decode.then(function (src) {
            var scale = Math.min(1, maxEdge / Math.max(src.width, src.height));
            var w = Math.max(1, Math.round(src.width * scale)), h = Math.max(1, Math.round(src.height * scale));
            var canvas = document.createElement('canvas');
            canvas.width = w; canvas.height = h;
            var ctx = canvas.getContext('2d');
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, w, h);
            ctx.drawImage(src, 0, 0, w, h);
            if (src.close) src.close();
            return new Promise(function (r) { canvas.toBlob(r, 'image/jpeg', quality); });
        }).then(function (blob) {
            if (!blob) throw new Error('encode');
            return blob.arrayBuffer();
        }).then(function (buffer) {
            done(true, new Int8Array(buffer));
        }).catch(function () { done(false, null); });
    });
    document.body.appendChild(input);
    input.click();
})""",
)
