package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JsDocumentPicker : DocumentPicker {
    override suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument? =
        suspendCancellableCoroutine { cont ->
            val onDone: (String?, dynamic) -> Unit = { name, bytes ->
                if (cont.isActive) cont.resume(if (name == null) null else PickedDocument(name, bytes.unsafeCast<ByteArray>()))
            }
            val onError: () -> Unit = { if (cont.isActive) cont.resumeWithException(IllegalStateException("Could not read file")) }
            openBrowserFilePicker(extensions, onDone, onError)
        }
}

private val openBrowserFilePicker: dynamic = js(
    """(function (accept, onDone, onError) {
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    var settled = false;
    var done = function (ok, name, value) {
        if (settled) return;
        settled = true;
        input.remove();
        ok ? onDone(name, value) : onError();
    };
    input.addEventListener('cancel', function () { done(true, null, null); });
    input.addEventListener('change', function () {
        var file = input.files && input.files[0];
        if (!file) { done(true, null, null); return; }
        file.arrayBuffer().then(function (buffer) { done(true, file.name, new Int8Array(buffer)); })
            .catch(function () { done(false, null, null); });
    });
    document.body.appendChild(input);
    input.click();
})""",
)
