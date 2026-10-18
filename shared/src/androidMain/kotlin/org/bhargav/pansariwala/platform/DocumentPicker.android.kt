package org.bhargav.pansariwala.platform

import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val KEY_DOCUMENT = "pansari_document_open"

class AndroidDocumentPicker : DocumentPicker {
    override suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument? {
        val context = AndroidActivityHolder.activity?.applicationContext ?: return null
        val uri = ActivityResultBridge.launch(KEY_DOCUMENT, ActivityResultContracts.OpenDocument(), mimeTypes.toTypedArray())
            ?: return null
        return withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: "products.xlsx"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not open file")
            PickedDocument(name, bytes)
        }
    }
}
