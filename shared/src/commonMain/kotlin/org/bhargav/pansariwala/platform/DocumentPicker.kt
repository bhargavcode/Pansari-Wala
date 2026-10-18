package org.bhargav.pansariwala.platform

class PickedDocument(val name: String, val bytes: ByteArray)

/** System file picker for non-image files (product spreadsheets). */
interface DocumentPicker {
    /** Null when the user cancels; throws on read failures. */
    suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument?
}

class UnavailableDocumentPicker : DocumentPicker {
    override suspend fun pickDocument(mimeTypes: List<String>, extensions: String): PickedDocument? = null
}
