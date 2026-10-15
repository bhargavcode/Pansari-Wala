package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.PickedImage
import org.bhargav.pansariwala.util.AppConstants
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WasmJsImagePicker : ImagePicker {
    override suspend fun pickImage(source: ImageSource): PickedImage? = suspendCancellableCoroutine { cont ->
        openBrowserImagePicker(
            capture = source == ImageSource.CAMERA,
            maxEdge = AppConstants.PHOTO_MAX_EDGE_PX,
            quality = AppConstants.PHOTO_JPEG_QUALITY / 100.0,
            onDone = { bytes ->
                if (cont.isActive) {
                    cont.resume(bytes?.let { PickedImage("photo.jpg", it.toByteArray(), "image/jpeg") })
                }
            },
            onError = {
                if (cont.isActive) cont.resumeWithException(ImagePickException("Could not read photo"))
            },
        )
    }
}

private fun JsAny.toByteArray(): ByteArray {
    val size = int8Length(this)
    return ByteArray(size) { int8At(this, it) }
}

private fun int8Length(array: JsAny): Int = js("array.length")

private fun int8At(array: JsAny, index: Int): Byte = js("array[index]")

/** Hidden `<input type=file>` + canvas downscale to JPEG; EXIF orientation is applied by the browser. */
private fun openBrowserImagePicker(
    capture: Boolean,
    maxEdge: Int,
    quality: Double,
    onDone: (JsAny?) -> Unit,
    onError: () -> Unit,
): Unit = js(
    """{
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    if (capture) input.setAttribute('capture', 'environment');
    input.style.display = 'none';
    let settled = false;
    const done = (ok, value) => {
        if (settled) return;
        settled = true;
        input.remove();
        ok ? onDone(value) : onError();
    };
    const loadImage = (file) => new Promise((resolve, reject) => {
        const url = URL.createObjectURL(file);
        const img = new Image();
        img.onload = () => { URL.revokeObjectURL(url); resolve(img); };
        img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('decode')); };
        img.src = url;
    });
    input.addEventListener('cancel', () => done(true, null));
    input.addEventListener('change', async () => {
        const file = input.files && input.files[0];
        if (!file) { done(true, null); return; }
        try {
            let src;
            try { src = await createImageBitmap(file, { imageOrientation: 'from-image' }); }
            catch (_) { src = await loadImage(file); }
            const w0 = src.width, h0 = src.height;
            const scale = Math.min(1, maxEdge / Math.max(w0, h0));
            const w = Math.max(1, Math.round(w0 * scale)), h = Math.max(1, Math.round(h0 * scale));
            const canvas = document.createElement('canvas');
            canvas.width = w; canvas.height = h;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, w, h);
            ctx.drawImage(src, 0, 0, w, h);
            if (src.close) src.close();
            const blob = await new Promise((r) => canvas.toBlob(r, 'image/jpeg', quality));
            if (!blob) throw new Error('encode');
            done(true, new Int8Array(await blob.arrayBuffer()));
        } catch (_) {
            done(false, null);
        }
    });
    document.body.appendChild(input);
    input.click();
}""",
)
