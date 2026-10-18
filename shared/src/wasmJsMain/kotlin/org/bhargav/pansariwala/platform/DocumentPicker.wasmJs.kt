package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WasmJsDocumentPicker : DocumentPicker {
    override suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument? =
        suspendCancellableCoroutine { cont ->
            openBrowserFilePicker(
                accept = extensions,
                onDone = { name, bytes ->
                    if (cont.isActive) cont.resume(if (name == null || bytes == null) null else PickedDocument(name, bytes.toBytes()))
                },
                onError = { if (cont.isActive) cont.resumeWithException(IllegalStateException("Could not read file")) },
            )
        }
}

private fun JsAny.toBytes(): ByteArray {
    val size = docLength(this)
    return ByteArray(size) { docByteAt(this, it) }
}

private fun docLength(array: JsAny): Int = js("array.length")

private fun docByteAt(array: JsAny, index: Int): Byte = js("array[index]")

private fun openBrowserFilePicker(accept: String, onDone: (String?, JsAny?) -> Unit, onError: () -> Unit): Unit = js(
    """{
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    let settled = false;
    const done = (ok, name, value) => {
        if (settled) return;
        settled = true;
        input.remove();
        ok ? onDone(name, value) : onError();
    };
    input.addEventListener('cancel', () => done(true, null, null));
    input.addEventListener('change', async () => {
        const file = input.files && input.files[0];
        if (!file) { done(true, null, null); return; }
        try { done(true, file.name, new Int8Array(await file.arrayBuffer())); }
        catch (_) { done(false, null, null); }
    });
    document.body.appendChild(input);
    input.click();
}""",
)
