package org.bhargav.pansariwala.master

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume

data class WebPickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

suspend fun pickWebImageFile(): WebPickedFile? = suspendCancellableCoroutine { cont ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/png,image/jpeg,image/jpg,image/webp,.png,.jpg,.jpeg,.webp"
    input.onchange = {
        val file = input.files?.item(0)
        if (file == null) {
            if (cont.isActive) cont.resume(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val buffer = reader.result as? ArrayBuffer
                if (buffer == null) {
                    if (cont.isActive) cont.resume(null)
                } else {
                    val arr = Int8Array(buffer)
                    val bytes = ByteArray(arr.length) { idx ->
                        arr.asDynamic()[idx] as Byte
                    }
                    if (cont.isActive) {
                        cont.resume(
                            WebPickedFile(
                                name = file.name,
                                mimeType = file.type.ifBlank { "image/jpeg" },
                                bytes = bytes,
                            ),
                        )
                    }
                }
            }
            reader.onerror = {
                if (cont.isActive) cont.resume(null)
            }
            reader.readAsArrayBuffer(file)
        }
    }
    input.click()
}
